package com.rianlucassb.liftform.core.domain.exception;

/**
 * Thrown when generating a pre-signed download URL via {@code VideoStorage#generateDownloadUrl} fails.
 */
public class VideoDownloadUrlGenerationException extends VideoStorageException {

    public VideoDownloadUrlGenerationException(String message) {
        super(message);
    }

    public VideoDownloadUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

