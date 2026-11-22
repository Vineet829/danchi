package com.danchi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OAuthStateServiceTest {
    private final OAuthStateService states = new OAuthStateService();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final String redirect = "https://danchi.example/google";

    @Test
    void stateConsumedOncePerBrowser() {
        String state = states.issue(redirect, request);
        assertThat(states.consume(state, new MockHttpServletRequest())).isEmpty();
        assertThat(states.consume("unknown-state", request)).isEmpty();
        assertThat(states.consume(state, request)).contains(redirect);
        assertThat(states.consume(state, request)).isEmpty();
    }
}
