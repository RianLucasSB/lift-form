package com.rianlucassb.liftform.core.domain.exception;

/**
 * Thrown when generating a pre-signed upload URL via {@code VideoStorage#generateUploadUrl} fails.
 */
public class VideoUploadUrlGenerationException extends VideoStorageException {

    public VideoUploadUrlGenerationException(String message) {
        super(message);
    }

    public VideoUploadUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

