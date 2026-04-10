package com.rianlucassb.liftform.core.domain.exception;

public class VideoStoreException extends VideoStorageException {

    public VideoStoreException(String message) {
        super(message);
    }

    public VideoStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

