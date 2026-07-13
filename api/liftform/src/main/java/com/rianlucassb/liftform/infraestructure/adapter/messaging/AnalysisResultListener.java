package com.rianlucassb.liftform.infraestructure.adapter.messaging;

import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCaseInput;
import com.rianlucassb.liftform.infraestructure.adapter.messaging.dto.AnalysisFinishedMessage;
import com.rianlucassb.liftform.infraestructure.config.messaging.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalysisResultListener {

    private final RecordAnalysisResultUseCase recordAnalysisResultUseCase;

    public AnalysisResultListener(RecordAnalysisResultUseCase recordAnalysisResultUseCase) {
        this.recordAnalysisResultUseCase = recordAnalysisResultUseCase;
    }

    @RabbitListener(queues = RabbitMQConfig.RESULTS_QUEUE)
    public void onMessage(AnalysisFinishedMessage message) {
        try {
            recordAnalysisResultUseCase.execute(new RecordAnalysisResultUseCaseInput(
                    message.videoAnalysisId(),
                    message.modelVersion(),
                    message.overallScore(),
                    message.feedback(),
                    message.rawFeatures()
            ));
        } catch (InvalidStatusTransitionException e) {
            log.warn("Analysis {} is already in a terminal state, ignoring duplicate result delivery",
                    message.videoAnalysisId());
        }
    }
}
