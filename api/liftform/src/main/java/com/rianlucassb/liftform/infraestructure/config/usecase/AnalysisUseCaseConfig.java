package com.rianlucassb.liftform.infraestructure.config.usecase;

import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseImpl;
import com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis.TransactionalConfirmVideoUploadUsecase;
import com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis.TransactionalCreateAnalysisUsecase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AnalysisUseCaseConfig {

    @Bean("createVideoAnalysisImpl")
    public CreateAnalysisUseCase createVideoAnalysis(
            VideoAnalysisRepository repository,
            VideoStorage videoStorage,
            UserRepository userRepository
    ) {
        return new CreateAnalysisUseCaseImpl(repository, videoStorage, userRepository);
    }

    @Bean
    @Primary
    public CreateAnalysisUseCase transactionalCreateVideoAnalysis(
            @Qualifier("createVideoAnalysisImpl") CreateAnalysisUseCase createVideoAnalysis
    ) {
        return new TransactionalCreateAnalysisUsecase(createVideoAnalysis);
    }

    @Bean("confirmVideoUploadImpl")
    public ConfirmVideoUploadUseCase confirmVideoUploadUseCase(
            EventPublisher eventPublisher,
            VideoAnalysisRepository videoAnalysisRepository

    ) {
        return new ConfirmVideoUploadUseCaseImpl(eventPublisher, videoAnalysisRepository);
    }

    @Bean
    @Primary
    public ConfirmVideoUploadUseCase transactionalconfirmVideoUploadUseCase(
            @Qualifier("confirmVideoUploadImpl") ConfirmVideoUploadUseCase confirmVideoUploadUseCase
    ) {
        return new TransactionalConfirmVideoUploadUsecase(confirmVideoUploadUseCase);
    }
}