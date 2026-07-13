package com.rianlucassb.liftform.infraestructure.adapter.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCaseInput;
import com.rianlucassb.liftform.infraestructure.config.messaging.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Consumes both the extraction and scoring DLQs. Both queues carry the same structured-JSON
 * shape when a worker's own retry loop explicitly publishes to them, but a message can still
 * arrive raw and unstructured via RabbitMQ's native dead-lettering (e.g. a crash before the
 * worker gets to its explicit publish) - this listener has to tolerate that case rather than
 * assume every message matches the contract.
 */
@Component
@Slf4j
public class PipelineErrorListener {

    private final RecordPipelineErrorUseCase recordPipelineErrorUseCase;
    private final ObjectMapper objectMapper;

    public PipelineErrorListener(RecordPipelineErrorUseCase recordPipelineErrorUseCase, ObjectMapper objectMapper) {
        this.recordPipelineErrorUseCase = recordPipelineErrorUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = {RabbitMQConfig.EXTRACTION_DLQ, RabbitMQConfig.SCORING_DLQ})
    public void onMessage(Message message) {
        String consumerQueue = message.getMessageProperties().getConsumerQueue();

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Dropping unparseable pipeline-error message from queue {}", consumerQueue, e);
            return;
        }

        Long analysisId = toAnalysisId(payload.get("analysisId"));
        if (analysisId == null) {
            log.error("Dropping pipeline-error message with no analysisId from queue {}: {}", consumerQueue, payload);
            return;
        }

        PipelineStage stage = resolveStage(payload.get("stage"), consumerQueue);
        String errorMessage = asString(payload.get("errorMessage"));
        String stackTrace = asString(payload.get("stackTrace"));

        try {
            recordPipelineErrorUseCase.execute(
                    new RecordPipelineErrorUseCaseInput(analysisId, stage, errorMessage, stackTrace)
            );
        } catch (InvalidStatusTransitionException e) {
            log.warn("Analysis {} is already in a terminal state, ignoring duplicate error delivery", analysisId);
        }
    }

    private Long toAnalysisId(Object raw) {
        if (raw == null) return null;
        try {
            return Long.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private PipelineStage resolveStage(Object rawStage, String consumerQueue) {
        if (rawStage != null) {
            try {
                return PipelineStage.valueOf(rawStage.toString());
            } catch (IllegalArgumentException ignored) {
                // fall through to queue-based default
            }
        }
        return RabbitMQConfig.SCORING_DLQ.equals(consumerQueue) ? PipelineStage.SCORING : PipelineStage.EXTRACTION;
    }

    private String asString(Object raw) {
        return raw == null ? null : raw.toString();
    }
}
