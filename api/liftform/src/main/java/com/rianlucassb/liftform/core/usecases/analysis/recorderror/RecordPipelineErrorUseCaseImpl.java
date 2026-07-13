package com.rianlucassb.liftform.core.usecases.analysis.recorderror;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;

public class RecordPipelineErrorUseCaseImpl implements RecordPipelineErrorUseCase {

    private final VideoAnalysisRepository videoAnalysisRepository;

    public RecordPipelineErrorUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository) {
        this.videoAnalysisRepository = videoAnalysisRepository;
    }

    @Override
    public void execute(RecordPipelineErrorUseCaseInput input) {
        VideoAnalysis videoAnalysis = videoAnalysisRepository.findById(input.analysisId())
                .orElseThrow(() -> new EntityNotFoundException("Video Analysis not found"));

        PipelineError error = new PipelineError(
                null,
                input.analysisId(),
                input.stage(),
                input.errorMessage(),
                input.stackTrace(),
                null
        );

        videoAnalysis.recordFailure(error);

        videoAnalysisRepository.save(videoAnalysis);
    }
}
