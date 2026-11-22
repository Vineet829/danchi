package com.danchi.common;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ContentViewRecorder.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContentViewTest {

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
    @Autowired private ContentViewRecorder views;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void clearFixtures() {
        database.execute("TRUNCATE users CASCADE");
        database.execute("TRUNCATE content_views");
    }

    @Test
    void signedInViewsCountOnce() throws Exception {
        User resident = user();
        long post = post(resident);
        together(2, i -> views.record("post", post, resident, new MockHttpServletRequest()));
        views.record("post", post, null, new MockHttpServletRequest());
        assertViews("post", post, 1);
    }

    @Test
    void deletionRemovesOnlyItsViews() {
        User author = user();
        long post = post(author);
        long remaining = post(author);
        views.record("post", post, author, new MockHttpServletRequest());
        views.record("post", remaining, author, new MockHttpServletRequest());
        database.update("DELETE FROM posts WHERE pkid = ?", post);
        assertViews("post", post, 0);
        assertViews("post", remaining, 1);

        long apartment = database.queryForObject("""
                INSERT INTO apartments (id, unit_number, building, floor, tenant_pkid)
                VALUES (?, 'QA-101', 'QA', 1, ?) RETURNING pkid
                """, Long.class, UUID.randomUUID(), author.getPkid());
        long issue = database.queryForObject("""
                INSERT INTO issues (id, apartment_pkid, reported_by_pkid, title, description)
                VALUES (?, ?, ?, 'QA issue', 'Cleanup test') RETURNING pkid
                """, Long.class, UUID.randomUUID(), apartment, author.getPkid());
        views.record("issue", issue, author, new MockHttpServletRequest());
        database.update("DELETE FROM apartments WHERE pkid = ?", apartment);
        assertViews("issue", issue, 0);
        assertViews("post", remaining, 1);
    }

    @Test
    void failedDeletionKeepsViews() {
        User author = user();
        long post = post(author);
        views.record("post", post, author, new MockHttpServletRequest());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            database.update("DELETE FROM posts WHERE pkid = ?", post);
            assertViews("post", post, 0);
            status.setRollbackOnly();
        });
        assertViews("post", post, 1);
    }

    @Test
    void lateViewsAfterDeletionIgnored() throws Exception {
        User author = user();
        long post = post(author);
        together(2, i -> {
            if (i == 0) {
                for (int j = 0; j < 10; j++) views.record("post", post, author, new MockHttpServletRequest());
            } else {
                database.update("DELETE FROM posts WHERE pkid = ?", post);
            }
        });
        views.record("post", post, author, new MockHttpServletRequest());
        assertViews("post", post, 0);
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

    private long post(User author) {
        UUID id = UUID.randomUUID();
        return database.queryForObject("""
                INSERT INTO posts (id, title, slug, body, author_pkid)
                VALUES (?, 'QA post', ?, 'Cleanup test', ?) RETURNING pkid
                """, Long.class, id, "qa-" + id, author.getPkid());
    }

    private void assertViews(String type, long pkid, int expected) {
        assertThat(database.queryForObject("SELECT count(*) FROM content_views WHERE content_type = ? AND object_pkid = ?",
                Integer.class, type, pkid)).isEqualTo(expected);
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
