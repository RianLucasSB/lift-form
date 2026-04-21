package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmailIgnoreCase(email)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsernameIgnoreCase(username)
                .map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
    }
}