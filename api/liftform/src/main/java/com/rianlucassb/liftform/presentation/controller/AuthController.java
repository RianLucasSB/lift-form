package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCase;
import com.rianlucassb.liftform.presentation.dto.LoginRequestDTO;
import com.rianlucassb.liftform.presentation.dto.LoginResponseDTO;
import com.rianlucassb.liftform.presentation.dto.RegisterRequestDTO;
import com.rianlucassb.liftform.presentation.dto.RegisterResponseDTO;
import com.rianlucassb.liftform.presentation.mapper.LoginUseCaseMapper;
import com.rianlucassb.liftform.presentation.mapper.RegisterUseCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final LoginUseCaseMapper loginUseCaseMapper;

    private final RegisterUseCase registerUseCase;
    private final RegisterUseCaseMapper registerUseCaseMapper;

    @PostMapping
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        var input = loginUseCaseMapper.toUseCaseInput(request);
        var output = loginUseCase.execute(input);
        var response = loginUseCaseMapper.toResponseDTO(output);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        var input = registerUseCaseMapper.toUseCaseInput(request);
        var output = registerUseCase.execute(input);
        var response = registerUseCaseMapper.toResponseDTO(output);
        return ResponseEntity.ok(response);
    }
}
