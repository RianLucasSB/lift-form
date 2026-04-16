package com.rianlucassb.liftform.presentation.mapper;

import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseOutput;
import com.rianlucassb.liftform.presentation.dto.CreateAnalysisResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreateAnalysisUseCaseMapper {
    CreateAnalysisResponseDTO toResponseDTO(CreateAnalysisUseCaseOutput output);
}
