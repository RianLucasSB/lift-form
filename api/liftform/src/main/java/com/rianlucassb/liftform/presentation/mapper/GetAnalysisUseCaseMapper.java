package com.rianlucassb.liftform.presentation.mapper;

import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.PipelineError;
import com.rianlucassb.liftform.core.usecases.analysis.getanalysis.GetAnalysisUseCaseOutput;
import com.rianlucassb.liftform.presentation.dto.AnalysisResultDTO;
import com.rianlucassb.liftform.presentation.dto.GetAnalysisResponseDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GetAnalysisUseCaseMapper {

    GetAnalysisResponseDTO toResponseDTO(GetAnalysisUseCaseOutput output);

    AnalysisResultDTO toResponseDTO(AnalysisResult result);

    default List<String> toErrorMessages(List<PipelineError> errors) {
        if (errors == null) return List.of();
        return errors.stream().map(PipelineError::errorMessage).toList();
    }
}
