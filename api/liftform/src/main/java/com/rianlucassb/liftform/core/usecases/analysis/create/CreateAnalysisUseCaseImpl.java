package com.rianlucassb.liftform.core.usecases.analysis.create;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public class CreateAnalysisUseCaseImpl implements CreateAnalysisUseCase {

    private static final Duration UPLOAD_EXPIRATION = Duration.ofMinutes(15);
    private static final int UPLOAD_EXPIRATION_SECONDS = (int) UPLOAD_EXPIRATION.getSeconds();

    private final VideoAnalysisRepository videoAnalysisRepository;
    private final VideoStorage videoStorage;

    public CreateAnalysisUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository, VideoStorage videoStorage) {
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.videoStorage = videoStorage;
    }

    @Override
    public CreateAnalysisUseCaseOutput execute(CreateAnalysisUseCaseInput input) {
        Objects.requireNonNull(input, "Input must not be null");
        Objects.requireNonNull(input.userId(), "userId must not be null");
        Objects.requireNonNull(input.exerciseType(), "exerciseType must not be null");

        if (input.userId().isBlank()) throw new IllegalArgumentException("userId must not be blank");
        if (input.exerciseType().isBlank()) throw new IllegalArgumentException("exerciseType must not be blank");

        UUID userId = UUID.fromString(input.userId()); // throws IllegalArgumentException for invalid UUIDs

        String key = String.format("videos/%s/%s/%s.mp4", input.exerciseType(), input.userId(), UUID.randomUUID());

        VideoAnalysis videoAnalysis = new VideoAnalysis(
                null,
                userId,
                input.exerciseType(),
                key,
                VideoAnalysisStatus.CREATED,
                null,
                null
        );

        VideoAnalysis saved = videoAnalysisRepository.save(videoAnalysis);

        String uploadUrl = videoStorage.generateUploadUrl(key, UPLOAD_EXPIRATION);

        return new CreateAnalysisUseCaseOutput(
                saved.id(),
                uploadUrl,
                UPLOAD_EXPIRATION_SECONDS
        );
    }
}
