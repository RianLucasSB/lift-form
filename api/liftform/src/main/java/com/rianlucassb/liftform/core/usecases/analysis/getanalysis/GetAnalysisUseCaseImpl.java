package com.rianlucassb.liftform.core.usecases.analysis.getanalysis;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GetAnalysisUseCaseImpl implements GetAnalysisUseCase {

    private final VideoAnalysisRepository videoAnalysisRepository;

    public GetAnalysisUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository) {
        this.videoAnalysisRepository = videoAnalysisRepository;
    }

    @Override
    public GetAnalysisUseCaseOutput execute(GetAnalysisUseCaseInput input) {
        Objects.requireNonNull(input.analysisId(), "Video Analysis id must not be null");

        VideoAnalysis videoAnalysis = videoAnalysisRepository.findByIdWithDetails(input.analysisId())
                .orElseThrow(() -> new EntityNotFoundException("Video Analysis not found"));

        if (!videoAnalysis.getUserId().equals(UUID.fromString(input.userId())))
            throw new EntityNotFoundException("Video Analysis not found");

        AnalysisResult result = videoAnalysis.getStatus() == VideoAnalysisStatus.COMPLETED
                ? videoAnalysis.getResult()
                : null;

        List<PipelineError> errors = videoAnalysis.getStatus() == VideoAnalysisStatus.FAILED
                ? videoAnalysis.getErrors()
                : List.of();

        return new GetAnalysisUseCaseOutput(videoAnalysis.getStatus(), result, errors);
    }
}
