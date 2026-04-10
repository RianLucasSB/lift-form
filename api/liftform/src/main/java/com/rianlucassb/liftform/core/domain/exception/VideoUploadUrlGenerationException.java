package com.rianlucassb.liftform.core.domain.exception;

public class VideoUploadUrlGenerationException extends VideoStorageException {

    public VideoUploadUrlGenerationException(String message) {
        super(message);
    }

    public VideoUploadUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

