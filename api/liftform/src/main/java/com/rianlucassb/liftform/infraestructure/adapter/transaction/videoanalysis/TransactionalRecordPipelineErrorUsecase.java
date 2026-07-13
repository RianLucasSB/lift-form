package com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis;

import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCaseInput;
import jakarta.transaction.Transactional;

public class TransactionalRecordPipelineErrorUsecase implements RecordPipelineErrorUseCase {

    private final RecordPipelineErrorUseCase recordPipelineErrorUseCase;

    public TransactionalRecordPipelineErrorUsecase(RecordPipelineErrorUseCase recordPipelineErrorUseCase) {
        this.recordPipelineErrorUseCase = recordPipelineErrorUseCase;
    }

    @Override
    @Transactional
    public void execute(RecordPipelineErrorUseCaseInput input) {
        recordPipelineErrorUseCase.execute(input);
    }
}
