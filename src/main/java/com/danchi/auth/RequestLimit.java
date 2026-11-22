package com.danchi.auth;

import java.time.Duration;

public enum RequestLimit {
    SIGN_IN_ADDRESS(30, Duration.ofMinutes(15), "Too many sign-in attempts. Please try again later."),
    SIGN_IN_EMAIL(10, Duration.ofMinutes(15), "Too many sign-in attempts. Please try again later."),
    SIGN_UP_ADDRESS(5, Duration.ofHours(1), "Too many sign-ups from this network. Please try again later."),
    ACCOUNT_EMAIL_ADDRESS(5, Duration.ofHours(1), "Too many emails requested. Please try again later."),
    ACCOUNT_EMAIL_RECIPIENT(3, Duration.ofHours(1), "Too many emails requested. Please try again later."),
    ACCOUNT_EMAIL_ALL(100, Duration.ofHours(1), "Too many emails requested. Please try again later."),
    GOOGLE_SIGN_IN_ADDRESS(20, Duration.ofMinutes(15), "Too many sign-in attempts. Please try again later.");

    final int most;
    final Duration window;
    final String refusal;

    RequestLimit(int most, Duration window, String refusal) {
        this.most = most;
        this.window = window;
        this.refusal = refusal;
    }
}
