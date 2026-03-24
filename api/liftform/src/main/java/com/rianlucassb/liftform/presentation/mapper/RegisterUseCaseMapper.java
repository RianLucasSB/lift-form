package com.rianlucassb.liftform.presentation.mapper;

import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseOutput;
import com.rianlucassb.liftform.presentation.dto.RegisterResponseDTO;
import org.mapstruct.Mapper;
import com.rianlucassb.liftform.presentation.dto.RegisterRequestDTO;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseInput;

@Mapper(componentModel = "spring")
public interface RegisterUseCaseMapper {
    RegisterUseCaseInput toUseCaseInput(RegisterRequestDTO dto);
    RegisterResponseDTO toResponseDTO(RegisterUseCaseOutput output);
}
