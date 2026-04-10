package com.rianlucassb.liftform.core.domain.exception;

/**
 * Thrown when persisting a video file via {@code VideoStorage#store} fails.
 */
public class VideoStoreException extends VideoStorageException {

    public VideoStoreException(String message) {
        super(message);
    }

    public VideoStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

