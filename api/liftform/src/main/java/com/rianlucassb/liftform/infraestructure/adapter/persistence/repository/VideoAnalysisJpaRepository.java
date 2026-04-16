package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.VideoAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoAnalysisJpaRepository extends JpaRepository<VideoAnalysisEntity, Long> {
}
