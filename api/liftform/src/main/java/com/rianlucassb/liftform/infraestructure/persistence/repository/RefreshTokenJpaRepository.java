package com.rianlucassb.liftform.infraestructure.persistence.repository;

import com.rianlucassb.liftform.infraestructure.persistence.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, String> {
    void deleteByUserId(String userId);
    Optional<RefreshTokenEntity> findByUserId(UUID userId);
    Optional<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);}
