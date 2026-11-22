package com.danchi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danchi.profile.Profile;
import com.danchi.profile.ProfileRepository;
import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

class GoogleOAuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final GoogleIdentityProvider google = mock(GoogleIdentityProvider.class);
    private final GoogleIdentityRepository identities = mock(GoogleIdentityRepository.class);
    private final PendingSignUps pending = mock(PendingSignUps.class);
    private final OAuthStateService states = new OAuthStateService();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final String redirect = "https://danchi.example/google";
    private final GoogleOAuthService oauth = new GoogleOAuthService(
            new GoogleOAuthProperties("client", "secret", List.of(redirect)), states, google, users, profiles, passwords, identities,
            pending);

    @Test
    void doesNotLinkPasswordAccounts() {
        when(google.exchange("code", redirect)).thenReturn(info("subject"));
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        String state = states.issue(redirect, request);
        assertThatThrownBy(() -> oauth.login("google-oauth2", state, "code", request))
                .isInstanceOf(GoogleAuthenticationException.class).hasMessageContaining("password");
        verify(users, never()).save(any());
        verify(identities, never()).save(any());
    }

    @Test
    void subjectLoginChecksAccountState() {
        User user = user();
        var identity = new GoogleIdentity();
        identity.setSubject("subject");
        identity.setUser(user);
        when(identities.findById("subject")).thenReturn(Optional.of(identity));
        when(google.exchange("code", redirect)).thenReturn(info("subject"));
        assertThat(oauth.login("google-oauth2", states.issue(redirect, request), "code", request)).isSameAs(user);
        verify(users, never()).existsByEmailIgnoreCase(anyString());
        user.setActive(false);
        assertThatThrownBy(() -> oauth.login("google-oauth2", states.issue(redirect, request), "code", request))
                .isInstanceOf(GoogleAuthenticationException.class);
        user.setActive(true);
        Profile profile = new Profile();
        profile.setReportCount(5);
        when(profiles.findByUserPkid(user.getPkid())).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> oauth.login("google-oauth2", states.issue(redirect, request), "code", request))
                .isInstanceOf(GoogleAuthenticationException.class);
    }

    @Test
    void clearsPendingSignUpFirst() {
        when(google.exchange("code", redirect)).thenReturn(info("new-subject"));
        when(passwords.encode(anyString())).thenReturn("unusable-random-password");
        when(users.save(any())).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setPkid(5L);
            return user;
        });
        oauth.login("google-oauth2", states.issue(redirect, request), "code", request);
        InOrder order = inOrder(pending, users);
        order.verify(pending).removeFor("person@example.com");
        order.verify(users).existsByEmailIgnoreCase("person@example.com");
    }

    @Test
    void newAccountStoresSubject() {
        when(google.exchange("code", redirect)).thenReturn(info("new-subject"));
        when(passwords.encode(anyString())).thenReturn("unusable-random-password");
        when(users.save(any())).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setPkid(4L);
            return user;
        });
        User created = oauth.login("google-oauth2", states.issue(redirect, request), "code", request);
        assertThat(created.isActive()).isTrue();
        verify(identities).save(argThat(identity -> identity.getSubject().equals("new-subject") && identity.getUser() == created));
    }

    private GoogleUserInfo info(String sub) {
        return new GoogleUserInfo("person@example.com", "Person", "Resident", sub, true);
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
