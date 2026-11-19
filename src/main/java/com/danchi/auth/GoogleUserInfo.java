package com.danchi.auth;

public record GoogleUserInfo(
        String email, String firstName, String lastName, String sub, boolean emailVerified) {}
