package com.rianlucassb.liftform.presentation.mapper;

import com.rianlucassb.liftform.presentation.dto.LoginRequestDTO;
import com.rianlucassb.liftform.presentation.dto.LoginResponseDTO;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCaseInput;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCaseOutput;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginUseCaseMapper {
    LoginResponseDTO toResponseDTO(LoginUseCaseOutput output);
    LoginUseCaseInput toUseCaseInput(LoginRequestDTO dto);
}
