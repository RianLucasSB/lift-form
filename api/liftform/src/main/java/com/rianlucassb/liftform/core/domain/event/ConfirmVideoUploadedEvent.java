package com.rianlucassb.liftform.core.domain.event;

public class ConfirmVideoUploadedEvent extends DomainEvent {
    private final Long videoAnalysisId;

    public ConfirmVideoUploadedEvent(Long videoAnalysisId) {
        this.videoAnalysisId = videoAnalysisId;
    }

    public Long getVideoAnalysisId() { return videoAnalysisId; }

    @Override
    public String getEventType() {
        return "VideoAnalysisUploaded";
    }
}
