package com.rianlucassb.liftform.infraestructure.adapter.persistence.mapper;

import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.AnalysisResultEntity;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.PipelineErrorEntity;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.VideoAnalysisEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class VideoAnalysisMapper {

    public VideoAnalysis toDomain(VideoAnalysisEntity entity) {
        return VideoAnalysis.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getExerciseType(),
                entity.getVideoS3Key(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null,
                null
        );
    }

    // with result and errors
    public VideoAnalysis toDomainWithDetails(VideoAnalysisEntity entity) {
        return VideoAnalysis.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getExerciseType(),
                entity.getVideoS3Key(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                toAnalysisResult(entity.getResult()),
                toPipelineErrors(entity.getErrors())
        );
    }

    public abstract VideoAnalysisEntity toEntity(VideoAnalysis domain);

    @AfterMapping
    protected void linkAssociations(@MappingTarget VideoAnalysisEntity entity) {
        if (entity.getResult() != null) {
            entity.getResult().setAnalysis(entity);
        }
        if (entity.getErrors() != null) {
            entity.getErrors().forEach(error -> error.setAnalysis(entity));
        }
    }

    protected AnalysisResultEntity toEntity(AnalysisResult result) {
        if (result == null) return null;
        return AnalysisResultEntity.builder()
                .id(result.id())
                .modelVersion(result.modelVersion())
                .overallScore(result.overallScore())
                .classification(result.classification())
                .feedback(result.feedback())
                .rawFeatures(result.rawFeatures())
                .scoredAt(result.scoredAt())
                .build();
    }

    protected List<PipelineErrorEntity> toPipelineErrorEntities(List<PipelineError> errors) {
        if (errors == null) return new ArrayList<>();
        return errors.stream().map(this::toEntity).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    protected PipelineErrorEntity toEntity(PipelineError error) {
        return PipelineErrorEntity.builder()
                .id(error.id())
                .stage(error.stage())
                .errorMessage(error.errorMessage())
                .stackTrace(error.stackTrace())
                .occurredAt(error.occurredAt())
                .build();
    }

    protected AnalysisResult toAnalysisResult(AnalysisResultEntity entity) {
        if (entity == null) return null;
        return new AnalysisResult(
                entity.getId(),
                entity.getAnalysis().getId(),
                entity.getModelVersion(),
                entity.getOverallScore(),
                entity.getClassification(),
                entity.getFeedback(),
                entity.getRawFeatures(),
                entity.getScoredAt()
        );
    }

    protected List<PipelineError> toPipelineErrors(List<PipelineErrorEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(this::toPipelineError)
                .toList();
    }

    protected PipelineError toPipelineError(PipelineErrorEntity entity) {
        return new PipelineError(
                entity.getId(),
                entity.getAnalysis().getId(),
                entity.getStage(),
                entity.getErrorMessage(),
                entity.getStackTrace(),
                entity.getOccurredAt()
        );
    }
}