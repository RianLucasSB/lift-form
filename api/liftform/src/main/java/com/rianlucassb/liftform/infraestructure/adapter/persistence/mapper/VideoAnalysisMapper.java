package com.rianlucassb.liftform.infraestructure.adapter.persistence.mapper;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.VideoAnalysisEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoAnalysisMapper {
    VideoAnalysis toDomain(VideoAnalysisEntity entity);
    VideoAnalysisEntity toEntity(VideoAnalysis domain);
}
