package com.rianlucassb.liftform.core.domain.exception;

class InvalidRefreshTokenException extends InvalidCredentialsException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}