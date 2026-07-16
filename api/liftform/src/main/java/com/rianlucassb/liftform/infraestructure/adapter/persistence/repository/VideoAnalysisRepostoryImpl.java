package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.mapper.VideoAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VideoAnalysisRepostoryImpl implements VideoAnalysisRepository {

    private final VideoAnalysisJpaRepository repository;
    private final VideoAnalysisMapper mapper;

    @Override
    public Optional<VideoAnalysis> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<VideoAnalysis> findByIdWithDetails(Long id) {
        return repository.findByIdWithDetails(id).map(mapper::toDomainWithDetails);
    }

    @Override
    public List<VideoAnalysis> findAllByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public VideoAnalysis save(VideoAnalysis videoAnalysis) {
        return mapper.toDomain(
                repository.save(mapper.toEntity(videoAnalysis))
        );
    }
}
