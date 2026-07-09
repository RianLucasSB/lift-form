package com.rianlucassb.liftform.core.domain.event;

public class ConfirmVideoUploadedEvent extends DomainEvent {
    private final Long videoAnalysisId;
    private final String s3Key;

    public ConfirmVideoUploadedEvent(Long videoAnalysisId, String s3Key) {
        this.videoAnalysisId = videoAnalysisId;
        this.s3Key = s3Key;
    }

    public Long getVideoAnalysisId() { return videoAnalysisId; }
    public String getS3Key() { return s3Key; }

    @Override
    public String getEventType() {
        return "VideoAnalysisUploaded";
    }
}
