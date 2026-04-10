package com.rianlucassb.liftform.core.gateway.analysis;

import java.time.Duration;

public interface VideoStorage {
    String store(String key, byte[] content);
    String generateUploadUrl(String key, Duration expiration);
    String generateDownloadUrl(String key, Duration expiration);
}