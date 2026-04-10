package com.rianlucassb.liftform.core.domain.exception;

/**
 * Base exception for all failures that originate from the video storage gateway.
 * Concrete sub-classes map 1-to-1 with each {@code VideoStorage} operation.
 */
public class VideoStorageException extends RuntimeException {

    public VideoStorageException(String message) {
        super(message);
    }

    public VideoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

