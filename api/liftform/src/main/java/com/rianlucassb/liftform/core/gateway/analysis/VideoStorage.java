package com.rianlucassb.liftform.core.gateway.analysis;

import com.rianlucassb.liftform.core.domain.exception.VideoDownloadUrlGenerationException;
import com.rianlucassb.liftform.core.domain.exception.VideoStoreException;
import com.rianlucassb.liftform.core.domain.exception.VideoUploadUrlGenerationException;

import java.time.Duration;

public interface VideoStorage {

    /**
     * Persists a video file under {@code key}.
     *
     * @throws VideoStoreException if the underlying storage operation fails.
     */
    String store(String key, byte[] content);

    /**
     * Generates a short-lived pre-signed URL that allows a client to upload a video.
     *
     * @throws VideoUploadUrlGenerationException if the URL cannot be generated.
     */
    String generateUploadUrl(String key, Duration expiration);

    /**
     * Generates a short-lived pre-signed URL that allows a client to download a video.
     *
     * @throws VideoDownloadUrlGenerationException if the URL cannot be generated.
     */
    String generateDownloadUrl(String key, Duration expiration);
}