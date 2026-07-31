package com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis;

import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCaseInput;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Primary;

@Primary
public class TransactionalConfirmVideoUploadUsecase implements ConfirmVideoUploadUseCase {

    private final ConfirmVideoUploadUseCase confirmVideoUploadUseCase;

    public TransactionalConfirmVideoUploadUsecase(ConfirmVideoUploadUseCase confirmVideoUploadUseCase) {
        this.confirmVideoUploadUseCase = confirmVideoUploadUseCase;
    }

    @Override
    @Transactional
    public void execute(ConfirmVideoUploadUseCaseInput input) {
        this.confirmVideoUploadUseCase.execute(input);
    }
}
