package com.danchi.issue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.danchi.auth.GuestService;
import com.danchi.issue.dto.IssueStatusUpdateRequest;
import com.danchi.mail.MailService;
import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {"spring.jpa.show-sql=false", "app.guest.enabled=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GuestService.class, IssueWriteService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IssueResolutionTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
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
    @Autowired private GuestService guests;
    @Autowired private IssueWriteService issues;
    @Autowired private UserRepository users;
    @MockBean private MailService mail;

    @Test
    void concurrentResolvesSendOneEmail() throws Exception {
        User guest = guests.create("198.51.100.1");
        User staff = resident();
        staff.setStaff(true);
        UUID issue = database.queryForObject(
                "SELECT id FROM issues WHERE reported_by_pkid=? AND status='reported'", UUID.class, guest.getPkid());

        together(() -> issues.updateStatus(issue, new IssueStatusUpdateRequest(null, null, IssueStatus.RESOLVED), staff));

        verify(mail, times(1)).send(eq(guest.getEmail()), eq("Issue Resolved"), anyString());
    }

    private void together(Runnable action) throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                await(start);
                action.run();
            });
            var second = pool.submit(() -> {
                await(start);
                action.run();
            });
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private User resident() {
        String name = "resident_" + UUID.randomUUID().toString().replace("-", "");
        long id = database.queryForObject("""
                INSERT INTO users(id,email,username,first_name,last_name,password,is_active)
                VALUES(?,?,?,'Test','Resident','!test',true) RETURNING pkid
                """, Long.class, UUID.randomUUID(), name + "@example.invalid", name);
        database.update("INSERT INTO profiles(id,user_pkid,slug) VALUES(?,?,?)", UUID.randomUUID(), id, name);
        return users.findById(id).orElseThrow();
    }
}
