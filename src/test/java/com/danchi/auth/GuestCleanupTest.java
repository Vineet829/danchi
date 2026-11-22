package com.danchi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.danchi.profile.AvatarStorage;
import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.util.UUID;
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
@Import({GuestService.class, GuestCleanup.class, RequestLimiter.class, PendingSignUps.class, AvatarStorage.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GuestCleanupTest {
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
    @Autowired private GuestCleanup cleanup;
    @Autowired private UserRepository users;
    @MockBean private TokenRevocations revocations;

    @Test
    void cleanupRemovesOnlyExpiredGuests() {
        User resident = resident();
        long post = post(resident);
        User first = guests.create("198.51.100.1");
        User second = guests.create("198.51.100.1");
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.isStaff() || first.isSuperuser()).isFalse();
        assertThat(first.getPassword()).startsWith("!");
        assertThat(count("SELECT count(*) FROM issues WHERE reported_by_pkid=?", first.getPkid())).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM apartments WHERE tenant_pkid=?", second.getPkid())).isEqualTo(1);
        database.update("INSERT INTO post_upvotes(post_pkid,user_pkid) VALUES (?,?),(?,?)",
                post, first.getPkid(), post, resident.getPkid());
        database.update("UPDATE posts SET upvotes=2 WHERE pkid=?", post);
        database.update("UPDATE users SET guest_expires_at=NOW()-INTERVAL '1 minute' WHERE pkid=?", first.getPkid());

        cleanup.removeExpired();

        assertThat(users.findById(first.getPkid())).isEmpty();
        assertThat(users.findById(second.getPkid())).isPresent();
        assertThat(users.findById(resident.getPkid())).isPresent();
        assertThat(count("SELECT count(*) FROM apartments WHERE tenant_pkid=?", first.getPkid())).isZero();
        assertThat(count("SELECT upvotes FROM posts WHERE pkid=?", post)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM issues WHERE reported_by_pkid=?", second.getPkid())).isEqualTo(2);
    }

    private int count(String sql, Object value) {
        return database.queryForObject(sql, Integer.class, value);
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

    private long post(User author) {
        return database.queryForObject("""
                INSERT INTO posts(id,title,slug,body,author_pkid) VALUES(?,'Post',?,'Body',?) RETURNING pkid
                """, Long.class, UUID.randomUUID(), UUID.randomUUID().toString(), author.getPkid());
    }
}
