package com.rianlucassb.liftform.infraestructure.adapter.persistence.mapper;

import com.rianlucassb.liftform.core.domain.model.RefreshToken;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.RefreshTokenEntity;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    @Mapping(source = "user.id", target = "userId")
    RefreshToken toDomain(RefreshTokenEntity entity);

    @Mapping(source = "userId", target = "user")
    RefreshTokenEntity toEntity(RefreshToken domain);

    default UserEntity map(UUID userId) {
        if (userId == null) return null;

        UserEntity user = new UserEntity();
        user.setId(userId);
        return user;
    }
}