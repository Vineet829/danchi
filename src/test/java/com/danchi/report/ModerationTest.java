package com.danchi.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.danchi.auth.GuestService;
import com.danchi.mail.MailService;
import com.danchi.profile.ProfileRepository;
import com.danchi.report.dto.ReportCreateRequest;
import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {"spring.jpa.show-sql=false", "app.guest.enabled=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReportWriteService.class, GuestService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ModerationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void cleanup() {
        POSTGRES.stop();
    }

    @Autowired private JdbcTemplate database;
    @Autowired private UserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private ReportWriteService reports;
    @Autowired private GuestService guests;
    @Autowired private PlatformTransactionManager transactions;
    @MockBean private MailService mail;

    @BeforeEach
    void clearFixtures() {
        database.execute("TRUNCATE users, guest_login_limits CASCADE");
        reset(mail);
    }

    @Test
    void repeatReportsCountOnce() {
        User target = user();
        User reporter = user();
        for (int i = 0; i < 10; i++) report(target, reporter);
        assertModeration(target, 1, true);
        assertThat(database.queryForObject("SELECT count(*) FROM reports", Integer.class)).isEqualTo(10);
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
        verify(mail, times(0)).send(eq(target.getEmail()), startsWith("Account Deactivation"), anyString());
    }

    @Test
    void fifthReporterDeactivatesOnce() {
        User target = user();
        for (int count = 1; count <= 6; count++) {
            report(target, user());
            assertModeration(target, count, count < 5);
        }
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Account Deactivation"), anyString());
    }

    @Test
    void selfReportsAreRejected() {
        User target = user();
        assertThatThrownBy(() -> report(target, target))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertModeration(target, 0, true);
        assertThat(database.queryForObject("SELECT count(*) FROM reports", Integer.class)).isZero();
        verifyNoInteractions(mail);
    }

    @Test
    void concurrentRepeatReportsCountOnce() throws Exception {
        User target = user();
        User reporter = user();
        together(10, i -> report(target, reporter));
        assertModeration(target, 1, true);
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
    }

    @Test
    void concurrentReportersAreAllCounted() throws Exception {
        User target = user();
        List<User> reporters = new ArrayList<>();
        for (int i = 0; i < 6; i++) reporters.add(user());
        together(6, i -> report(target, reporters.get(i)));
        assertModeration(target, 6, false);
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Warning:"), anyString());
        verify(mail, times(1)).send(eq(target.getEmail()), startsWith("Account Deactivation"), anyString());
    }

    @Test
    void rolledBackReportChangesNothing() {
        User target = user();
        User reporter = user();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            report(target, reporter);
            status.setRollbackOnly();
        });
        assertModeration(target, 0, true);
        verify(mail, times(0)).send(anyString(), anyString(), anyString());
    }

    @Test
    void staleEditKeepsNewerValues() {
        User target = user();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var staleProfile = profiles.findByUserPkid(target.getPkid()).orElseThrow();
            var staleUser = users.findById(target.getPkid()).orElseThrow();
            var concurrent = new TransactionTemplate(transactions);
            concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            concurrent.executeWithoutResult(other -> {
                database.update("UPDATE profiles SET avatar = '/media/avatars/new.png', report_count = 5, reputation = 0 WHERE user_pkid = ?", target.getPkid());
                database.update("UPDATE users SET is_active = false WHERE pkid = ?", target.getPkid());
            });
            staleProfile.setBio("Updated bio");
            staleUser.setFirstName("Updated name");
        });
        assertModeration(target, 5, false);
        var profile = profiles.findByUserPkid(target.getPkid()).orElseThrow();
        assertThat(profile.getAvatar()).isEqualTo("/media/avatars/new.png");
        assertThat(profile.getBio()).isEqualTo("Updated bio");
        assertThat(users.findById(target.getPkid()).orElseThrow().getFirstName()).isEqualTo("Updated name");
    }

    @Test
    void guestReportsDoNotCount() {
        User target = user();
        for (int i = 0; i < 5; i++) {
            User guest = guests.create("198.51.100." + i);
            reports.create(new ReportCreateRequest("Guest report " + i, "Demo", target.getUsername()), guest);
        }
        assertThat(reportCount(target)).isZero();
        verifyNoInteractions(mail);
        for (int i = 0; i < 5; i++) {
            reports.create(new ReportCreateRequest("Resident report " + i, "Incident", target.getUsername()), user());
        }
        assertThat(reportCount(target)).isEqualTo(5);
        assertThat(users.findById(target.getPkid()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void reportedStaffStaysActive() {
        User staff = user();
        database.update("UPDATE users SET is_staff=TRUE WHERE pkid=?", staff.getPkid());
        for (int i = 0; i < 5; i++) {
            reports.create(new ReportCreateRequest("Report " + i, "Incident", staff.getUsername()), user());
        }
        assertThat(reportCount(staff)).isEqualTo(5);
        assertThat(users.findById(staff.getPkid()).orElseThrow().isActive()).isTrue();
    }

    private User user() {
        String name = "qa_" + UUID.randomUUID().toString().replace("-", "");
        long pkid = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password, is_active)
                VALUES (?, ?, ?, 'QA', 'Resident', '!unused', true) RETURNING pkid
                """, Long.class, UUID.randomUUID(), name + "@example.invalid", name);
        database.update("INSERT INTO profiles (id, user_pkid, slug) VALUES (?, ?, ?)", UUID.randomUUID(), pkid, name);
        return users.findById(pkid).orElseThrow();
    }

    private void report(User target, User reporter) {
        reports.create(new ReportCreateRequest("QA " + UUID.randomUUID(), "QA incident", target.getUsername()), reporter);
    }

    private int reportCount(User user) {
        return database.queryForObject("SELECT report_count FROM profiles WHERE user_pkid=?", Integer.class, user.getPkid());
    }

    private void assertModeration(User target, int count, boolean active) {
        var profile = profiles.findByUserPkid(target.getPkid()).orElseThrow();
        assertThat(profile.getReportCount()).isEqualTo(count);
        assertThat(profile.getReputation()).isEqualTo(Math.max(0, 100 - count * 20));
        assertThat(users.findById(target.getPkid()).orElseThrow().isActive()).isEqualTo(active);
    }

    private void together(int count, java.util.function.IntConsumer action) throws Exception {
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(count)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    action.accept(index);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> task : tasks) task.get(20, TimeUnit.SECONDS);
        }
    }
}
