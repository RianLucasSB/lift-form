package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseInput;
import com.rianlucassb.liftform.infraestructure.config.security.JWTUserData;
import com.rianlucassb.liftform.presentation.constants.ApiPaths;
import com.rianlucassb.liftform.presentation.dto.CreateAnalysisRequestDTO;
import com.rianlucassb.liftform.presentation.dto.CreateAnalysisResponseDTO;
import com.rianlucassb.liftform.presentation.mapper.CreateAnalysisUseCaseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/analysis")
@RequiredArgsConstructor
public class VideoAnalysisController {

    private final CreateAnalysisUseCase createAnalysisUseCase;

    private final CreateAnalysisUseCaseMapper createAnalysisUseCaseMapper;

    @PostMapping("/create")
    public ResponseEntity<CreateAnalysisResponseDTO> requestAnalysis(
            @AuthenticationPrincipal JWTUserData userdata,
            @Valid @RequestBody CreateAnalysisRequestDTO request
    ){
        CreateAnalysisUseCaseInput input = new CreateAnalysisUseCaseInput(
                userdata.id(),
                request.exerciseType(),
                request.fileName()
        );

        var response = createAnalysisUseCase.execute(input);

        return ResponseEntity.ok().body(createAnalysisUseCaseMapper.toResponseDTO(response));
    }
}
