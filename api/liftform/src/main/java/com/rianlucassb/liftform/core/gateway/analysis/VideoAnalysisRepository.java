package com.rianlucassb.liftform.core.gateway.analysis;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;

public interface VideoAnalysisRepository {
    VideoAnalysis save(VideoAnalysis videoAnalysis);
}
