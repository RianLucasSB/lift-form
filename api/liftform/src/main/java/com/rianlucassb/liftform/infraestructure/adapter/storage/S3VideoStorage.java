package com.rianlucassb.liftform.infraestructure.adapter.storage;

import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

public class S3VideoStorage implements VideoStorage {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucket;

    public S3VideoStorage(S3Presigner presigner, S3Client s3Client, String bucket) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String generateUploadUrl(String key, Duration expiration) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("video/mp4")
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .putObjectRequest(putObjectRequest)
            .signatureDuration(expiration)
            .build();

        return presigner.presignPutObject(presignRequest)
            .url()
            .toString();
    }

    @Override
    public String generateDownloadUrl(String key, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .getObjectRequest(getObjectRequest)
            .signatureDuration(expiration)
            .build();

        return presigner.presignGetObject(presignRequest)
            .url()
            .toString();
    }

    @Override
    public String store(String key, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("video/mp4")
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        return key;
    }
}