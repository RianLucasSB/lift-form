package com.rianlucassb.liftform.core.usecases.analysis.create;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;

import java.util.UUID;

public class CreateAnalysisUseCaseImpl implements CreateAnalysisUseCase {
    private final VideoAnalysisRepository videoAnalysisRepository;
    private final VideoStorage videoStorage;

    public CreateAnalysisUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository, VideoStorage videoStorage) {
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.videoStorage = videoStorage;
    }

    @Override
    public CreateAnalysisUseCaseOutput execute(CreateAnalysisUseCaseInput input) {
        String key = String.format("videos/%s/%s/%s.mp4", input.exerciseType(), input.userId(), UUID.randomUUID());

        VideoAnalysis videoAnalysis = new VideoAnalysis(
                null,
                UUID.fromString(input.userId()),
                input.exerciseType(),
                key,
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
