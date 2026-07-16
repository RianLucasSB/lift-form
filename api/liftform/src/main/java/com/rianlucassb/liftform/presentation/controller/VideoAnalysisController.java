package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCaseInput;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseInput;
import com.rianlucassb.liftform.core.usecases.analysis.getanalysis.GetAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.getanalysis.GetAnalysisUseCaseInput;
import com.rianlucassb.liftform.core.usecases.analysis.listanalyses.ListAnalysesUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.listanalyses.ListAnalysesUseCaseInput;
import com.rianlucassb.liftform.infraestructure.config.security.JWTUserData;
import com.rianlucassb.liftform.presentation.constants.ApiPaths;
import com.rianlucassb.liftform.presentation.dto.AnalysisSummaryDTO;
import com.rianlucassb.liftform.presentation.dto.CreateAnalysisRequestDTO;
import com.rianlucassb.liftform.presentation.dto.CreateAnalysisResponseDTO;
import com.rianlucassb.liftform.presentation.dto.GetAnalysisResponseDTO;
import com.rianlucassb.liftform.presentation.mapper.CreateAnalysisUseCaseMapper;
import com.rianlucassb.liftform.presentation.mapper.GetAnalysisUseCaseMapper;
import com.rianlucassb.liftform.presentation.mapper.ListAnalysesUseCaseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.V1 + "/analysis")
@RequiredArgsConstructor
public class VideoAnalysisController {

    private final CreateAnalysisUseCase createAnalysisUseCase;
    private final ConfirmVideoUploadUseCase confirmVideoUploadUseCase;
    private final GetAnalysisUseCase getAnalysisUseCase;
    private final ListAnalysesUseCase listAnalysesUseCase;

    private final CreateAnalysisUseCaseMapper createAnalysisUseCaseMapper;
    private final GetAnalysisUseCaseMapper getAnalysisUseCaseMapper;
    private final ListAnalysesUseCaseMapper listAnalysesUseCaseMapper;

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

    @PostMapping("/videos/{id}/upload-complete")
    public ResponseEntity<Void> confirmVideoUpload(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal JWTUserData userdata
    ){
        ConfirmVideoUploadUseCaseInput input = new ConfirmVideoUploadUseCaseInput(id, userdata.id());
        confirmVideoUploadUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetAnalysisResponseDTO> getAnalysis(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal JWTUserData userdata
    ){
        GetAnalysisUseCaseInput input = new GetAnalysisUseCaseInput(id, userdata.id());

        var response = getAnalysisUseCase.execute(input);

        return ResponseEntity.ok().body(getAnalysisUseCaseMapper.toResponseDTO(response));
    }

    @GetMapping
    public ResponseEntity<List<AnalysisSummaryDTO>> listAnalyses(
            @AuthenticationPrincipal JWTUserData userdata
    ){
        ListAnalysesUseCaseInput input = new ListAnalysesUseCaseInput(userdata.id());

        var response = listAnalysesUseCase.execute(input);

        return ResponseEntity.ok().body(listAnalysesUseCaseMapper.toResponseDTOs(response.analyses()));
    }
}
