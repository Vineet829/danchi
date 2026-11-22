package com.danchi.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AvatarService.class, AvatarStorage.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AvatarUploadTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final Path AVATARS;
    static {
        try {
            AVATARS = Files.createTempDirectory("danchi-avatar-test-");
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.media.avatar-dir", AVATARS::toString);
    }

    @AfterAll
    static void cleanup() throws Exception {
        POSTGRES.stop();
        try (var files = Files.list(AVATARS)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
        Files.deleteIfExists(AVATARS);
    }

    @Autowired private JdbcTemplate database;
    @Autowired private UserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private AvatarService avatars;
    @Autowired private AvatarStorage storage;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void clearFixtures() throws Exception {
        database.execute("TRUNCATE users CASCADE");
        try (var files = Files.list(AVATARS)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
    }

    @Test
    void newUploadReplacesOldFile() throws Exception {
        User user = user();
        for (String format : List.of("jpeg", "png", "gif", "webp")) {
            byte[] content = image(format);
            String old = avatar(user);
            avatars.upload(user, upload(format, content));
            String current = avatar(user);
            assertThat(Files.readAllBytes(path(current))).isEqualTo(content);
            if (old != null) assertThat(path(old)).doesNotExist();
            try (var files = Files.list(AVATARS)) {
                assertThat(files.count()).isEqualTo(1);
            }
        }
    }

    @Test
    void invalidImagesAreRejected() throws Exception {
        User user = user();
        List<MockMultipartFile> invalid = List.of(
                upload("png", "not an image".getBytes()),
                upload("jpeg", image("png")),
                upload("png", new byte[0]),
                new MockMultipartFile("avatar", "test.svg", "image/svg+xml", "<svg/>".getBytes()));
        for (var file : invalid) {
            assertThatThrownBy(() -> avatars.upload(user, file))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThatThrownBy(() -> avatars.upload(user, upload("png", new byte[1024 * 1024 + 1])))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
        assertThat(avatar(user)).isNull();
        try (var files = Files.list(AVATARS)) {
            assertThat(files.count()).isZero();
        }
    }

    @Test
    void hugeDimensionsRejectedBeforeDecoding() throws Exception {
        byte[] png = image("png");
        java.nio.ByteBuffer.wrap(png, 16, 8).putInt(5000).putInt(5000);
        var crc = new java.util.zip.CRC32();
        crc.update(png, 12, 17);
        java.nio.ByteBuffer.wrap(png, 29, 4).putInt((int) crc.getValue());
        User user = user();
        assertThatThrownBy(() -> avatars.upload(user, upload("png", png)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getReason()).contains("16 million pixels"));
        assertThat(avatar(user)).isNull();
    }

    @Test
    void avatarUploadStripsMetadata() throws Exception {
        User user = user();
        byte[] photo = image("jpeg");
        byte[] comment = "Taken at 51.5007 N 0.1246 W".getBytes(StandardCharsets.ISO_8859_1);
        var tagged = new ByteArrayOutputStream();
        tagged.write(photo, 0, 2);
        tagged.write(new byte[] {(byte) 0xFF, (byte) 0xFE, 0, (byte) (comment.length + 2)});
        tagged.write(comment);
        tagged.write(photo, 2, photo.length - 2);

        avatars.upload(user, upload("jpeg", tagged.toByteArray()));

        assertThat(new String(Files.readAllBytes(path(avatar(user))), StandardCharsets.ISO_8859_1))
                .doesNotContain("51.5007");
    }

    @Test
    void rollbackRemovesUncommittedFile() throws Exception {
        User user = user();
        avatars.upload(user, upload("png", image("png")));
        String original = avatar(user);
        String reference = UUID.randomUUID() + ".png";
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            storage.store(profilePkid(user), reference, new byte[] {1, 2, 3});
            assertThat(AVATARS.resolve(reference)).exists();
            status.setRollbackOnly();
        });
        assertThat(avatar(user)).isEqualTo(original);
        assertThat(path(original)).exists();
        assertThat(AVATARS.resolve(reference)).doesNotExist();
    }

    @Test
    void storageFailureKeepsExistingFile() throws Exception {
        User user = user();
        avatars.upload(user, upload("png", image("png")));
        String original = avatar(user);
        byte[] bytes = Files.readAllBytes(path(original));
        assertThatThrownBy(() -> storage.store(profilePkid(user), path(original).getFileName().toString(), new byte[] {1}))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(avatar(user)).isEqualTo(original);
        assertThat(Files.readAllBytes(path(original))).isEqualTo(bytes);
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

    private String avatar(User user) {
        return profiles.findByUserPkid(user.getPkid()).orElseThrow().getAvatar();
    }

    private Long profilePkid(User user) {
        return profiles.findByUserPkid(user.getPkid()).orElseThrow().getPkid();
    }

    private Path path(String avatar) {
        return AVATARS.resolve(avatar.substring("/media/avatars/".length()));
    }

    private byte[] image(String format) throws Exception {
        if (format.equals("webp")) {
            try (var input = getClass().getResourceAsStream("/avatar.webp")) {
                return input.readAllBytes();
            }
        }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), format, bytes);
        return bytes.toByteArray();
    }

    private MockMultipartFile upload(String format, byte[] bytes) {
        return new MockMultipartFile("avatar", "photo." + format, "image/" + format, bytes);
    }
}
