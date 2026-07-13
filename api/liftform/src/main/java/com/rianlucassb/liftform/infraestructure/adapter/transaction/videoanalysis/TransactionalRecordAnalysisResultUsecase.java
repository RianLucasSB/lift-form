package com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis;

import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCaseInput;
import jakarta.transaction.Transactional;

public class TransactionalRecordAnalysisResultUsecase implements RecordAnalysisResultUseCase {

    private final RecordAnalysisResultUseCase recordAnalysisResultUseCase;

    public TransactionalRecordAnalysisResultUsecase(RecordAnalysisResultUseCase recordAnalysisResultUseCase) {
        this.recordAnalysisResultUseCase = recordAnalysisResultUseCase;
    }

    @Override
    @Transactional
    public void execute(RecordAnalysisResultUseCaseInput input) {
        recordAnalysisResultUseCase.execute(input);
    }
}
