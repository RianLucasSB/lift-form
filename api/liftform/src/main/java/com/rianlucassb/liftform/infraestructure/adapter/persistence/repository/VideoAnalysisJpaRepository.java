package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.VideoAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoAnalysisJpaRepository extends JpaRepository<VideoAnalysisEntity, Long> {

    @Query("SELECT va FROM VideoAnalysisEntity va " +
            "LEFT JOIN FETCH va.result " +
            "LEFT JOIN FETCH va.errors " +
            "WHERE va.id = :id")
    Optional<VideoAnalysisEntity> findByIdWithDetails(@Param("id") Long id);

    List<VideoAnalysisEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
