package com.rianlucassb.liftform.application.service;

import com.rianlucassb.liftform.application.dto.LoginResponseDTO;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.usecases.AuthenticateUserUseCase;
import com.rianlucassb.liftform.infraestructure.config.security.TokenService;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final TokenService tokenService;

    public AuthApplicationService(AuthenticateUserUseCase useCase,
                                  TokenService tokenService) {
        this.authenticateUserUseCase = useCase;
        this.tokenService = tokenService;
    }

    public LoginResponseDTO login(String login, String password) {

        User user = authenticateUserUseCase.execute(login, password);

        String accessToken = tokenService.generateToken(user);
        String refreshToken = "TODO"; // Implement refresh token generation

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}