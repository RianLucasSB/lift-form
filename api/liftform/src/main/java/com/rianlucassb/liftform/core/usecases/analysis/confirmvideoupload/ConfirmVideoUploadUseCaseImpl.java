package com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload;

import com.rianlucassb.liftform.core.domain.event.ConfirmVideoUploadedEvent;
import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.exception.FileTooLargeException;
import com.rianlucassb.liftform.core.domain.exception.UnsupportedFileTypeException;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoObjectMetadata;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;

import java.util.Objects;
import java.util.UUID;

public class ConfirmVideoUploadUseCaseImpl implements ConfirmVideoUploadUseCase {

    private final EventPublisher eventPublisher;
    private final VideoAnalysisRepository videoAnalysisRepository;
    private final VideoStorage videoStorage;
    private final long maxSizeBytes;
    private final String allowedContentType;

    public ConfirmVideoUploadUseCaseImpl(
            EventPublisher eventPublisher,
            VideoAnalysisRepository videoAnalysisRepository,
            VideoStorage videoStorage,
            long maxSizeBytes,
            String allowedContentType
    ) {
        this.eventPublisher = eventPublisher;
        this.videoAnalysisRepository = videoAnalysisRepository;
        this.videoStorage = videoStorage;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedContentType = allowedContentType;
    }

    @Override
    public void execute(ConfirmVideoUploadUseCaseInput input) {
        Objects.requireNonNull(input.videoAnalysisId(), "Video Analysis id must not be null");

        VideoAnalysis videoAnalysis = videoAnalysisRepository.findById(input.videoAnalysisId())
                .orElseThrow(() -> new EntityNotFoundException("Video Analysis not found"));

        if(!videoAnalysis.getUserId().equals(UUID.fromString(input.userId())))
            throw new EntityNotFoundException("Video Analysis not found");

        if (videoAnalysis.getStatus() == VideoAnalysisStatus.CREATED) {
            validateUploadedObject(videoAnalysis);
        }

        videoAnalysis.confirmUpload();

        videoAnalysisRepository.save(videoAnalysis);

        ConfirmVideoUploadedEvent event = new ConfirmVideoUploadedEvent(input.videoAnalysisId(), videoAnalysis.getVideoS3Key());

        eventPublisher.publish(event);
    }

    private void validateUploadedObject(VideoAnalysis videoAnalysis) {
        VideoObjectMetadata metadata = videoStorage.getObjectMetadata(videoAnalysis.getVideoS3Key());

        if (metadata.contentLength() > maxSizeBytes) {
            videoStorage.deleteObject(videoAnalysis.getVideoS3Key());
            throw new FileTooLargeException(
                    "Uploaded file (%d bytes) exceeds the maximum allowed size of %d bytes"
                            .formatted(metadata.contentLength(), maxSizeBytes)
            );
        }

        if (!allowedContentType.equals(metadata.contentType())) {
            videoStorage.deleteObject(videoAnalysis.getVideoS3Key());
            throw new UnsupportedFileTypeException(
                    "Uploaded file content type '%s' is not supported, expected '%s'"
                            .formatted(metadata.contentType(), allowedContentType)
            );
        }
    }
}
