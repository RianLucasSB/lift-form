package com.rianlucassb.liftform.core.domain.exception;

public class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message, Exception e) {
        super(message, e);
    }
}
