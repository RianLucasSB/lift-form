package com.rianlucassb.liftform.core.usecases.analysis.recordresult;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;

public class RecordAnalysisResultUseCaseImpl implements RecordAnalysisResultUseCase {

    private final VideoAnalysisRepository videoAnalysisRepository;

    public RecordAnalysisResultUseCaseImpl(VideoAnalysisRepository videoAnalysisRepository) {
        this.videoAnalysisRepository = videoAnalysisRepository;
    }

    @Override
    public void execute(RecordAnalysisResultUseCaseInput input) {
        VideoAnalysis videoAnalysis = videoAnalysisRepository.findById(input.videoAnalysisId())
                .orElseThrow(() -> new EntityNotFoundException("Video Analysis not found"));

        AnalysisResult result = AnalysisResult.score(
                input.videoAnalysisId(),
                input.modelVersion(),
                input.overallScore(),
                input.feedback(),
                input.rawFeatures()
        );

        videoAnalysis.markCompleted(result);

        videoAnalysisRepository.save(videoAnalysis);
    }
}
