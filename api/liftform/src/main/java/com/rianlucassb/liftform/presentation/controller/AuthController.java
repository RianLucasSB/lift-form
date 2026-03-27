package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCase;
import com.rianlucassb.liftform.presentation.constants.ApiPaths;
import com.rianlucassb.liftform.presentation.dto.LoginRequestDTO;
import com.rianlucassb.liftform.presentation.dto.LoginResponseDTO;
import com.rianlucassb.liftform.presentation.dto.RegisterRequestDTO;
import com.rianlucassb.liftform.presentation.dto.RegisterResponseDTO;
import com.rianlucassb.liftform.presentation.mapper.LoginUseCaseMapper;
import com.rianlucassb.liftform.presentation.mapper.RegisterUseCaseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final LoginUseCaseMapper loginUseCaseMapper;

    private final RegisterUseCase registerUseCase;
    private final RegisterUseCaseMapper registerUseCaseMapper;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        var input = loginUseCaseMapper.toUseCaseInput(request);
        var output = loginUseCase.execute(input);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", output.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(10))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginUseCaseMapper.toResponseDTO(output));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid RegisterRequestDTO request) {
        var input = registerUseCaseMapper.toUseCaseInput(request);
        var output = registerUseCase.execute(input);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", output.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(10))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(registerUseCaseMapper.toResponseDTO(output));
    }
}
