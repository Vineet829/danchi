package com.danchi.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.danchi.user.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostService.class)
class PostServiceTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Autowired private PostService responses;
    @Autowired private PostRepository posts;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate database;

    @Test
    void pagedListsKeepEveryTag() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User author = user("tagger_" + suffix);
        long garden = tag("garden-" + suffix);
        long noise = tag("noise-" + suffix);
        for (int i = 0; i < 3; i++) {
            UUID postId = post(author);
            for (long tag : List.of(garden, noise)) {
                database.update("INSERT INTO post_tags (post_pkid, tag_pkid) VALUES ((SELECT pkid FROM posts WHERE id = ?), ?)",
                        postId, tag);
            }
        }
        entityManager.flush();
        entityManager.clear();

        var page = posts.findByAuthor(author.getPkid(), PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(responses.toResponses(page, author).getContent())
                .allSatisfy(post -> assertThat(post.tags()).containsExactly("garden-" + suffix, "noise-" + suffix));
        var tagged = posts.findByTagSlug("garden-" + suffix, PageRequest.of(0, 10));
        assertThat(responses.toTagResponses(tagged, author).getContent())
                .hasSize(3)
                .allSatisfy(post -> assertThat(post.tags()).containsExactly("garden-" + suffix, "noise-" + suffix));
    }

    @Test
    void listShowsReplyCountOnly() {
        User author = user("lister_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        UUID postId = post(author);
        for (int i = 0; i < 2; i++) {
            database.update("""
                    INSERT INTO replies (id, post_pkid, author_pkid, body)
                    VALUES (?, (SELECT pkid FROM posts WHERE id = ?), ?, 'A reply')
                    """, UUID.randomUUID(), postId, author.getPkid());
        }
        Post stored = posts.findByExternalId(postId).orElseThrow();

        var listed = responses.toResponses(new PageImpl<>(List.of(stored)), author).getContent().getFirst();
        assertThat(listed.repliesCount()).isEqualTo(2);
        assertThat(listed.replies()).isEmpty();
        assertThat(responses.toResponse(stored, author).replies()).hasSize(2);
    }

    private long tag(String name) {
        return database.queryForObject("INSERT INTO tags (name, slug) VALUES (?, ?) RETURNING pkid", Long.class, name, name);
    }

    private User user(String username) {
        Long pkid = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password)
                VALUES (?, ?, ?, 'Test', 'Author', '!unused') RETURNING pkid
                """, Long.class, UUID.randomUUID(), username + "@example.test", username);
        return entityManager.find(User.class, pkid);
    }

    private UUID post(User author) {
        UUID id = UUID.randomUUID();
        database.update("""
                INSERT INTO posts (id, title, slug, body, author_pkid)
                VALUES (?, 'List test', ?, 'Test post', ?)
                """, id, "list-" + id, author.getPkid());
        return id;
    }
}
