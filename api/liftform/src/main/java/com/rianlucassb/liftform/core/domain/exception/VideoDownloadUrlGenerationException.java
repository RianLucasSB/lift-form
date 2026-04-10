package com.rianlucassb.liftform.core.domain.exception;

public class VideoDownloadUrlGenerationException extends VideoStorageException {

    public VideoDownloadUrlGenerationException(String message) {
        super(message);
    }

    public VideoDownloadUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

