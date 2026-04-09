package com.rianlucassb.liftform.core.usecases.analysis.create;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;

import java.util.UUID;

public class CreateAnalysisUseCaseImpl implements CreateAnalysisUseCase {
    private final VideoAnalysisRepository videoAnalysisRepository;

    public CreateAnalysisUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository) {
        this.videoAnalysisRepository = videoAnalysisRepository;
    }

    @Override
    public CreateAnalysisUseCaseOutput execute(CreateAnalysisUseCaseInput input) {
        String s3Key = String.format("videos/%s/%s.mp4", input.userId(), UUID.randomUUID());

        VideoAnalysis videoAnalysis = new VideoAnalysis(
                null,
                UUID.fromString(input.userId()),
                input.exerciseType(),
                s3Key,
                VideoAnalysisStatus.CREATED,
                null,
                null
        );

        VideoAnalysis saved = videoAnalysisRepository.save(videoAnalysis);

        return new CreateAnalysisUseCaseOutput(
                saved.id(),
                "https://example.com/upload",
                3600
        );
    }
}
