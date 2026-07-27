package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.core.usecases.user.getcurrentuser.GetCurrentUserUseCase;
import com.rianlucassb.liftform.core.usecases.user.getcurrentuser.GetCurrentUserUseCaseInput;
import com.rianlucassb.liftform.infraestructure.config.security.JWTUserData;
import com.rianlucassb.liftform.presentation.constants.ApiPaths;
import com.rianlucassb.liftform.presentation.dto.CurrentUserResponseDTO;
import com.rianlucassb.liftform.presentation.mapper.GetCurrentUserUseCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/users")
@RequiredArgsConstructor
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetCurrentUserUseCaseMapper getCurrentUserUseCaseMapper;

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDTO> getCurrentUser(
            @AuthenticationPrincipal JWTUserData userdata
    ) {
        var input = new GetCurrentUserUseCaseInput(userdata.id());
        var output = getCurrentUserUseCase.execute(input);

        return ResponseEntity.ok().body(getCurrentUserUseCaseMapper.toResponseDTO(output));
    }
}
