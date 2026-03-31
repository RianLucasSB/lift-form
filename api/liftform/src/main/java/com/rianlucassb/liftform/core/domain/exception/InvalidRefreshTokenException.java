package com.rianlucassb.liftform.core.domain.exception;

public class InvalidRefreshTokenException extends InvalidCredentialsException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}