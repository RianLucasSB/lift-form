package com.rianlucassb.liftform.core.usecases.analysis.listanalyses;

import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListAnalysesUseCaseImpl implements ListAnalysesUseCase {

    private final VideoAnalysisRepository videoAnalysisRepository;

    public ListAnalysesUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository) {
        this.videoAnalysisRepository = videoAnalysisRepository;
    }

    @Override
    public ListAnalysesUseCaseOutput execute(ListAnalysesUseCaseInput input) {
        Objects.requireNonNull(input.userId(), "User id must not be null");

        List<AnalysisSummary> summaries = videoAnalysisRepository.findAllByUserId(UUID.fromString(input.userId()))
                .stream()
                .map(analysis -> new AnalysisSummary(
                        analysis.getId(),
                        analysis.getExerciseType(),
                        analysis.getStatus(),
                        analysis.getCreatedAt(),
                        analysis.getUpdatedAt()
                ))
                .toList();

        return new ListAnalysesUseCaseOutput(summaries);
    }
}
