package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.core.usecases.user.login.LoginUseCase;
import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCase;
import com.rianlucassb.liftform.core.usecases.user.refreshtoken.RefreshTokenUseCaseInput;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCase;
import com.rianlucassb.liftform.core.usecases.user.register.RegisterUseCaseOutput;
import com.rianlucassb.liftform.presentation.constants.ApiPaths;
import com.rianlucassb.liftform.presentation.dto.*;
import com.rianlucassb.liftform.presentation.mapper.LoginUseCaseMapper;
import com.rianlucassb.liftform.presentation.mapper.RegisterUseCaseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final LoginUseCaseMapper loginUseCaseMapper;
    private final RefreshTokenUseCase refreshTokenUseCase;

    private final RegisterUseCase registerUseCase;
    private final RegisterUseCaseMapper registerUseCaseMapper;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        var input = loginUseCaseMapper.toUseCaseInput(request);
        var output = loginUseCase.execute(input);

        ResponseCookie refreshCookie = getRefreshCookie(output.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginUseCaseMapper.toResponseDTO(output));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid RegisterRequestDTO request) {
        var input = registerUseCaseMapper.toUseCaseInput(request);
        var output = registerUseCase.execute(input);

        ResponseCookie refreshCookie = getRefreshCookie(output.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(registerUseCaseMapper.toResponseDTO(output));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@CookieValue (name = "refresh_token", required = false) String refreshToken) {
        var input = new RefreshTokenUseCaseInput(refreshToken);
        var output = refreshTokenUseCase.execute(input);

        ResponseCookie refreshCookie = getRefreshCookie(output.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new RefreshTokenResponseDTO(output.accessToken()));
    }

    private @NonNull ResponseCookie getRefreshCookie(String output) {
        return ResponseCookie.from("refresh_token", output)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(10))
                .sameSite("Strict")
                .build();
    }
}
