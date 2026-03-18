package com.rianlucassb.liftform.infraestructure.presentation;

import com.rianlucassb.liftform.application.dto.LoginRequestDTO;
import com.rianlucassb.liftform.application.dto.LoginResponseDTO;
import com.rianlucassb.liftform.application.service.AuthApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authApplicationService.login(request.login(), request.password());
        return ResponseEntity.ok(response);
    }
}
