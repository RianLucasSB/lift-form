package com.rianlucassb.liftform.core.domain.model;

import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VideoAnalysis {

    private final Long id;
    private final UUID userId;
    private final ExerciseType exerciseType;
    private final String videoS3Key;
    private VideoAnalysisStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private AnalysisResult result;
    private List<PipelineError> errors;

    private VideoAnalysis(UUID userId, ExerciseType exerciseType, String videoS3Key) {
        this.id = null;
        this.userId = userId;
        this.exerciseType = exerciseType;
        this.videoS3Key = videoS3Key;
        this.status = VideoAnalysisStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.errors = new ArrayList<>();
    }

    private VideoAnalysis(
            Long id,
            UUID userId,
            ExerciseType exerciseType,
            String videoS3Key,
            VideoAnalysisStatus status,
            Instant createdAt,
            Instant updatedAt,
            AnalysisResult result,
            List<PipelineError> errors
    ) {
        this.id = id;
        this.userId = userId;
        this.exerciseType = exerciseType;
        this.videoS3Key = videoS3Key;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.result = result;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public static VideoAnalysis initialize(UUID userId, ExerciseType exerciseType, String videoS3Key) {
        return new VideoAnalysis(userId, exerciseType, videoS3Key);
    }

    public static VideoAnalysis reconstitute(
            Long id,
            UUID userId,
            ExerciseType exerciseType,
            String videoS3Key,
            VideoAnalysisStatus status,
            Instant createdAt,
            Instant updatedAt,
            AnalysisResult result,
            List<PipelineError> errors
    ) {
        return new VideoAnalysis(id, userId, exerciseType, videoS3Key, status, createdAt, updatedAt, result, errors);
    }

    public void confirmUpload() {
        validateTransition(VideoAnalysisStatus.CREATED);
        this.status = VideoAnalysisStatus.UPLOADED;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(AnalysisResult result) {
        validateTransition(VideoAnalysisStatus.UPLOADED);
        this.result = result;
        this.status = VideoAnalysisStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void recordFailure(PipelineError error) {
        validateTransition(VideoAnalysisStatus.UPLOADED);
        this.errors.add(error);
        this.status = VideoAnalysisStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void validateTransition(VideoAnalysisStatus expected) {
        if (this.status != expected) {
            throw new InvalidStatusTransitionException(
                    "Invalid transition, expected %s but was %s"
                            .formatted(expected, this.status)
            );
        }
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public ExerciseType getExerciseType() { return exerciseType; }
    public String getVideoS3Key() { return videoS3Key; }
    public VideoAnalysisStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public AnalysisResult getResult() { return result; }
    public List<PipelineError> getErrors() { return Collections.unmodifiableList(errors); }
}