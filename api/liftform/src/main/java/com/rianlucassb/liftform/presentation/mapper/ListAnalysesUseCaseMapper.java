package com.rianlucassb.liftform.presentation.mapper;

import com.rianlucassb.liftform.core.usecases.analysis.listanalyses.AnalysisSummary;
import com.rianlucassb.liftform.presentation.dto.AnalysisSummaryDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ListAnalysesUseCaseMapper {

    AnalysisSummaryDTO toResponseDTO(AnalysisSummary summary);

    List<AnalysisSummaryDTO> toResponseDTOs(List<AnalysisSummary> summaries);
}
