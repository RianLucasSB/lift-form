package com.rianlucassb.liftform.infraestructure.adapter.storage;

import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("S3VideoStorage Integration Tests")
class S3VideoStorageIT extends AbstractIntegrationTest {

    @Autowired
    private VideoStorage videoStorage;

    // ----------------------------------------------------------------- store
    @Test
    @DisplayName("store() uploads content to S3 and returns the key")
    void store_uploadsContent_returnsKey() {
        String key = "videos/SQUAT/user123/test.mp4";
        byte[] content = "fake-video-content".getBytes();

        String returnedKey = videoStorage.store(key, content);

        assertThat(returnedKey).isEqualTo(key);
    }

    // ------------------------------------------------ generateUploadUrl
    @Test
    @DisplayName("generateUploadUrl() returns a non-blank pre-signed URL")
    void generateUploadUrl_returnsPresignedUrl() {
        String key = "videos/SQUAT/user123/upload.mp4";
        Duration expiration = Duration.ofMinutes(15);

        String url = videoStorage.generateUploadUrl(key, expiration);

        assertThat(url).isNotBlank();
    }

    @Test
    @DisplayName("generateUploadUrl() returns different URLs for different keys")
    void generateUploadUrl_differentKeys_returnsDifferentUrls() {
        String url1 = videoStorage.generateUploadUrl("videos/SQUAT/a/1.mp4", Duration.ofMinutes(5));
        String url2 = videoStorage.generateUploadUrl("videos/SQUAT/b/2.mp4", Duration.ofMinutes(5));

        assertThat(url1).isNotEqualTo(url2);
    }

    // ----------------------------------------------- generateDownloadUrl
    @Test
    @DisplayName("generateDownloadUrl() returns a non-blank pre-signed URL after object is stored")
    void generateDownloadUrl_afterStore_returnsPresignedUrl() {
        String key = "videos/SQUAT/user456/download.mp4";
        videoStorage.store(key, "video-bytes".getBytes());

        String url = videoStorage.generateDownloadUrl(key, Duration.ofMinutes(15));

        assertThat(url).isNotBlank();
    }

    @Test
    @DisplayName("generateDownloadUrl() pre-signed URL is accessible via HTTP GET against LocalStack")
    void generateDownloadUrl_presignedUrl_isAccessible() throws Exception {
        String key = "videos/SQUAT/user789/accessible.mp4";
        byte[] content = "downloadable-content".getBytes();
        videoStorage.store(key, content);

        String downloadUrl = videoStorage.generateDownloadUrl(key, Duration.ofMinutes(10));

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<byte[]> response = client.send(
                HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(content);
    }
}


