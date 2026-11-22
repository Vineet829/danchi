package com.danchi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danchi.profile.Profile;
import com.danchi.profile.ProfileRepository;
import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);

    @Test
    void inactiveAccountChecksPassword() {
        User user = user();
        user.setActive(false);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "supplied"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwords).matches("supplied", user.getPassword());
    }

    @Test
    void reportedStaffCanLogIn() {
        User user = user();
        user.setStaff(true);
        Profile profile = new Profile();
        profile.setReportCount(5);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(profiles.findByUserPkid(user.getPkid())).thenReturn(Optional.of(profile));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThat(auth(passwords).authenticate(user.getEmail(), "supplied")).isSameAs(user);
    }

    @Test
    void bannedAccountCannotSignIn() {
        User user = user();
        Profile profile = new Profile();
        profile.setReportCount(5);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByExternalId(user.getId())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(profiles.findByUserPkid(user.getPkid())).thenReturn(Optional.of(profile));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        var auth = auth(passwords);
        assertThatThrownBy(() -> auth.authenticate(user.getEmail(), "supplied")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.requireActiveUser(user.getId())).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void expiredGuestIsRejected() {
        User user = user();
        user.setGuest(true);
        user.setGuestExpiresAt(Instant.now().minusSeconds(1));
        when(users.findByExternalId(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth(passwords).requireActiveUser(user.getId()))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.isSessionEligible()).isFalse();
        var realEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth(realEncoder).authenticate(user.getEmail(), "attempt"))
                .isInstanceOf(InvalidCredentialsException.class);
        user.setGuestExpiresAt(Instant.now().plusSeconds(300));
        assertThat(user.isSessionEligible()).isTrue();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "supplied"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void passwordCheckedBeforeTransaction() {
        User user = user();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getPkid())).thenReturn(Optional.of(user));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThat(auth(passwords).authenticate(user.getEmail(), "supplied")).isSameAs(user);
        assertThat(user.getLastLogin()).isNotNull();
        InOrder order = inOrder(passwords, transactions, users);
        order.verify(passwords).matches("supplied", user.getPassword());
        order.verify(transactions).getTransaction(any());
        order.verify(users).findById(user.getPkid());
        order.verify(transactions).commit(any());
    }

    @Test
    void rejectsWrongOrChangedPassword() {
        User user = user();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(transactions, never()).getTransaction(any());

        User changed = user();
        changed.setPassword("new-hash");
        when(users.findById(user.getPkid())).thenReturn(Optional.of(changed));
        when(passwords.matches("supplied", user.getPassword())).thenReturn(true);
        assertThatThrownBy(() -> auth(passwords).authenticate(user.getEmail(), "supplied"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private AuthService auth(PasswordEncoder encoder) {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new AuthService(users, profiles, encoder, transactions);
    }

    private User user() {
        User user = new User();
        user.setPkid(1L);
        user.setId(UUID.randomUUID());
        user.setEmail("person@example.com");
        user.setUsername("person");
        user.setFirstName("Person");
        user.setLastName("Resident");
        user.setPassword("hash");
        user.setActive(true);
        return user;
    }
}
