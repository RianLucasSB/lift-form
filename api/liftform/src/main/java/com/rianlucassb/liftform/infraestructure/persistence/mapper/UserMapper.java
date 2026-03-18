package com.rianlucassb.liftform.infraestructure.persistence.mapper;

import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.infraestructure.persistence.entities.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);
}