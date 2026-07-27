package com.rianlucassb.liftform.infraestructure.config.usecase;

import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.analysis.VideoStorage;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.confirmvideoupload.ConfirmVideoUploadUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.create.CreateAnalysisUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.analysis.getanalysis.GetAnalysisUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.getanalysis.GetAnalysisUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.analysis.listanalyses.ListAnalysesUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.listanalyses.ListAnalysesUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCaseImpl;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCaseImpl;
import com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis.TransactionalConfirmVideoUploadUsecase;
import com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis.TransactionalCreateAnalysisUsecase;
import com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis.TransactionalRecordAnalysisResultUsecase;
import com.rianlucassb.liftform.infraestructure.adapter.transaction.videoanalysis.TransactionalRecordPipelineErrorUsecase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
            VideoAnalysisRepository videoAnalysisRepository,
            VideoStorage videoStorage,
            @Value("${analysis.upload.max-size-bytes:524288000}") long maxSizeBytes,
            @Value("${analysis.upload.allowed-content-type:video/mp4}") String allowedContentType
    ) {
        return new ConfirmVideoUploadUseCaseImpl(eventPublisher, videoAnalysisRepository, videoStorage, maxSizeBytes, allowedContentType);
    }

    @Bean
    @Primary
    public ConfirmVideoUploadUseCase transactionalconfirmVideoUploadUseCase(
            @Qualifier("confirmVideoUploadImpl") ConfirmVideoUploadUseCase confirmVideoUploadUseCase
    ) {
        return new TransactionalConfirmVideoUploadUsecase(confirmVideoUploadUseCase);
    }

    @Bean("recordAnalysisResultImpl")
    public RecordAnalysisResultUseCase recordAnalysisResultUseCase(
            VideoAnalysisRepository videoAnalysisRepository
    ) {
        return new RecordAnalysisResultUseCaseImpl(videoAnalysisRepository);
    }

    @Bean
    @Primary
    public RecordAnalysisResultUseCase transactionalRecordAnalysisResultUseCase(
            @Qualifier("recordAnalysisResultImpl") RecordAnalysisResultUseCase recordAnalysisResultUseCase
    ) {
        return new TransactionalRecordAnalysisResultUsecase(recordAnalysisResultUseCase);
    }

    @Bean
    public GetAnalysisUseCase getAnalysisUseCase(
            VideoAnalysisRepository videoAnalysisRepository
    ) {
        return new GetAnalysisUseCaseImpl(videoAnalysisRepository);
    }

    @Bean
    public ListAnalysesUseCase listAnalysesUseCase(
            VideoAnalysisRepository videoAnalysisRepository
    ) {
        return new ListAnalysesUseCaseImpl(videoAnalysisRepository);
    }

    @Bean("recordPipelineErrorImpl")
    public RecordPipelineErrorUseCase recordPipelineErrorUseCase(
            VideoAnalysisRepository videoAnalysisRepository
    ) {
        return new RecordPipelineErrorUseCaseImpl(videoAnalysisRepository);
    }

    @Bean
    @Primary
    public RecordPipelineErrorUseCase transactionalRecordPipelineErrorUseCase(
            @Qualifier("recordPipelineErrorImpl") RecordPipelineErrorUseCase recordPipelineErrorUseCase
    ) {
        return new TransactionalRecordPipelineErrorUsecase(recordPipelineErrorUseCase);
    }
}