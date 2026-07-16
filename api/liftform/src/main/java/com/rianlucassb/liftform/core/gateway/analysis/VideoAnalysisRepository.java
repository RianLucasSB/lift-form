package com.rianlucassb.liftform.core.gateway.analysis;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoAnalysisRepository {
    Optional<VideoAnalysis> findById(Long id);

    Optional<VideoAnalysis> findByIdWithDetails(Long id);

    List<VideoAnalysis> findAllByUserId(UUID userId);

    VideoAnalysis save(VideoAnalysis videoAnalysis);
}
