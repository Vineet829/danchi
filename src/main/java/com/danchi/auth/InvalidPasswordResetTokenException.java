package com.danchi.auth;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("Invalid token for given user.");
    }
}
