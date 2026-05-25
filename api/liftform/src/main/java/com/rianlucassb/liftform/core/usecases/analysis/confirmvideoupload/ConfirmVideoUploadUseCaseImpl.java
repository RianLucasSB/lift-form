package com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload;

import com.rianlucassb.liftform.core.domain.event.ConfirmVideoUploadedEvent;
import com.rianlucassb.liftform.core.domain.exception.VideoAnalysisNotFoundException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;

import java.util.Objects;

public class ConfirmVideoUploadUseCaseImpl implements ConfirmVideoUploadUseCase {

    private final EventPublisher eventPublisher;
    private final VideoAnalysisRepository videoAnalysisRepository;

    public ConfirmVideoUploadUseCaseImpl(EventPublisher eventPublisher, VideoAnalysisRepository videoAnalysisRepository) {
        this.eventPublisher = eventPublisher;
        this.videoAnalysisRepository = videoAnalysisRepository;
    }

    @Override
    public void execute(ConfirmVideoUploadUseCaseInput input) {
        Objects.requireNonNull(input.videoAnalysisId(), "Video Analysis id must not be null");

        VideoAnalysis videoAnalysis = videoAnalysisRepository.findById(input.videoAnalysisId())
                .orElseThrow(() -> new VideoAnalysisNotFoundException("Video Analysis not found"));

        videoAnalysis.confirmUpload();

        ConfirmVideoUploadedEvent event = new ConfirmVideoUploadedEvent(input.videoAnalysisId());

        eventPublisher.publish(event);
    }
}

