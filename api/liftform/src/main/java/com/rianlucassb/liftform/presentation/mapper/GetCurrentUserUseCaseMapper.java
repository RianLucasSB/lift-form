package com.rianlucassb.liftform.presentation.mapper;

import com.rianlucassb.liftform.core.usecases.user.getcurrentuser.GetCurrentUserUseCaseOutput;
import com.rianlucassb.liftform.presentation.dto.CurrentUserResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GetCurrentUserUseCaseMapper {

    CurrentUserResponseDTO toResponseDTO(GetCurrentUserUseCaseOutput output);
}
