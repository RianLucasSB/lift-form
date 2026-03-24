package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.presentation.dto.LoginRequestDTO;
import com.rianlucassb.liftform.presentation.dto.LoginResponseDTO;
import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.presentation.mapper.LoginUseCaseMapper;
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

    @PostMapping
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        var input = loginUseCaseMapper.toUseCaseInput(request);
        var output = loginUseCase.execute(input);
        var response = loginUseCaseMapper.toResponseDTO(output);
        return ResponseEntity.ok(response);
    }
}
