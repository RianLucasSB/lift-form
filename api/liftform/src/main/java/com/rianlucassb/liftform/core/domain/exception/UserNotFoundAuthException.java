package com.rianlucassb.liftform.core.domain.exception;

public class UserNotFoundAuthException extends InvalidCredentialsException {
    public UserNotFoundAuthException(String message) {
        super(message);
    }
}
