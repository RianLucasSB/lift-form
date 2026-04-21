package com.rianlucassb.liftform.infraestructure.config;

import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.infraestructure.adapter.storage.S3VideoStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@TestConfiguration
public class S3TestConfig {

    static final String TEST_BUCKET = "test-bucket";

    @Bean
    @Primary
    public S3Client testS3Client(LocalStackContainer localStackContainer) {
        S3Client client = S3Client.builder()
                .region(Region.of(localStackContainer.getRegion()))
                .endpointOverride(localStackContainer.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
        } catch (BucketAlreadyOwnedByYouException ignored) {}
        return client;
    }

    @Bean
    @Primary
    public S3Presigner testS3Presigner(LocalStackContainer localStackContainer) {
        return S3Presigner.builder()
                .region(Region.of(localStackContainer.getRegion()))
                .endpointOverride(localStackContainer.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    @Primary
    public VideoStorage testVideoStorage(S3Presigner testS3Presigner, S3Client testS3Client) {
        return new S3VideoStorage(testS3Presigner, testS3Client, TEST_BUCKET);
    }
}


