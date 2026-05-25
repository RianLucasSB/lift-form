package com.rianlucassb.liftform.core.gateway.analysis;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;

import java.util.Optional;

public interface VideoAnalysisRepository {
    Optional<VideoAnalysis> findById(Long id);

    VideoAnalysis save(VideoAnalysis videoAnalysis);
}
