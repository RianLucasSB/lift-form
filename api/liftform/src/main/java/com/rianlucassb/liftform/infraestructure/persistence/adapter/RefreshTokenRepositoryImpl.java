package com.rianlucassb.liftform.infraestructure.persistence.adapter;

import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.core.gateway.security.RefreshTokenRepository;
import com.rianlucassb.liftform.infraestructure.persistence.mapper.RefreshTokenMapper;
import com.rianlucassb.liftform.infraestructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    @Override
    public Optional<RefreshToken> findByHashedToken(String hashedToken) {
        return jpaRepository.findById(hashedToken)
                .map(mapper::toDomain);
    }

    @Override
    public void save(RefreshToken token) {
        jpaRepository.save(mapper.toEntity(token));
    }

    @Override
    public Optional<RefreshToken> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }
}
