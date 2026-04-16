package com.rianlucassb.liftform.infraestructure.config.usecase;

import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseImpl;
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
            VideoStorage videoStorage
    ) {
        return new CreateAnalysisUseCaseImpl(repository, videoStorage);
    }

    @Bean
    @Primary
    public CreateAnalysisUseCase transactionalCreateVideoAnalysis(
            @Qualifier("createVideoAnalysisImpl") CreateAnalysisUseCase createVideoAnalysis
    ) {
        return new TransactionalCreateAnalysisUsecase(createVideoAnalysis);
    }
}