package com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis;

import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseInput;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseOutput;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Primary;

@Primary
public class TransactionalCreateAnalysisUsecase implements CreateAnalysisUseCase {

    private final CreateAnalysisUseCase createAnalysisUseCase;

    public TransactionalCreateAnalysisUsecase(CreateAnalysisUseCase createAnalysisUseCase) {
        this.createAnalysisUseCase = createAnalysisUseCase;
    }

    @Override
    @Transactional
    public CreateAnalysisUseCaseOutput execute(CreateAnalysisUseCaseInput input) {
        return createAnalysisUseCase.execute(input);
    }
}
