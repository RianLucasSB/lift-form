package com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload;

import com.rianlucassb.liftform.core.domain.event.ConfirmVideoUploadedEvent;
import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;

import java.util.Objects;
import java.util.UUID;

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
                .orElseThrow(() -> new EntityNotFoundException("Video Analysis not found"));

        if(!videoAnalysis.getUserId().equals(UUID.fromString(input.userId())))
            throw new EntityNotFoundException("Video Analysis not found");

        videoAnalysis.confirmUpload();

        ConfirmVideoUploadedEvent event = new ConfirmVideoUploadedEvent(input.videoAnalysisId(), videoAnalysis.getVideoS3Key());

        eventPublisher.publish(event);
    }
}

