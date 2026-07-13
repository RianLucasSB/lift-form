package com.rianlucassb.liftform.infraestructure.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recorderror.RecordPipelineErrorUseCaseInput;
import com.rianlucassb.liftform.infraestructure.config.messaging.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineErrorListenerTest {

    @Mock private RecordPipelineErrorUseCase recordPipelineErrorUseCase;

    private PipelineErrorListener listener;

    @BeforeEach
    void setUp() {
        listener = new PipelineErrorListener(recordPipelineErrorUseCase, new ObjectMapper());
    }

    @Test
    @DisplayName("Should parse a well-formed structured error and execute the use case")
    void shouldParseStructuredErrorAndExecuteUseCase() {
        var message = messageFrom(
                RabbitMQConfig.SCORING_DLQ,
                """
                {"analysisId": 1, "stage": "SCORING", "errorMessage": "boom", "stackTrace": "trace"}
                """
        );

        listener.onMessage(message);

        ArgumentCaptor<RecordPipelineErrorUseCaseInput> captor = ArgumentCaptor.forClass(RecordPipelineErrorUseCaseInput.class);
        verify(recordPipelineErrorUseCase).execute(captor.capture());

        assertThat(captor.getValue().analysisId()).isEqualTo(1L);
        assertThat(captor.getValue().stage()).isEqualTo(PipelineStage.SCORING);
        assertThat(captor.getValue().errorMessage()).isEqualTo("boom");
        assertThat(captor.getValue().stackTrace()).isEqualTo("trace");
    }

    @Test
    @DisplayName("Should default the stage from the extraction DLQ when the payload is missing it")
    void shouldDefaultStageFromExtractionQueueWhenMissing() {
        var message = messageFrom(
                RabbitMQConfig.EXTRACTION_DLQ,
                """
                {"analysisId": 1, "errorMessage": "boom", "stackTrace": "trace"}
                """
        );

        listener.onMessage(message);

        ArgumentCaptor<RecordPipelineErrorUseCaseInput> captor = ArgumentCaptor.forClass(RecordPipelineErrorUseCaseInput.class);
        verify(recordPipelineErrorUseCase).execute(captor.capture());

        assertThat(captor.getValue().stage()).isEqualTo(PipelineStage.EXTRACTION);
    }

    @Test
    @DisplayName("Should default the stage from the scoring DLQ when the payload is missing it")
    void shouldDefaultStageFromScoringQueueWhenMissing() {
        var message = messageFrom(
                RabbitMQConfig.SCORING_DLQ,
                """
                {"analysisId": 1, "errorMessage": "boom", "stackTrace": "trace"}
                """
        );

        listener.onMessage(message);

        ArgumentCaptor<RecordPipelineErrorUseCaseInput> captor = ArgumentCaptor.forClass(RecordPipelineErrorUseCaseInput.class);
        verify(recordPipelineErrorUseCase).execute(captor.capture());

        assertThat(captor.getValue().stage()).isEqualTo(PipelineStage.SCORING);
    }

    @Test
    @DisplayName("Should drop the message and never call the use case when analysisId is missing")
    void shouldDropMessageWithoutAnalysisId() {
        var message = messageFrom(
                RabbitMQConfig.EXTRACTION_DLQ,
                """
                {"errorMessage": "boom"}
                """
        );

        listener.onMessage(message);

        verify(recordPipelineErrorUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should drop the message and never call the use case when the body is unparseable JSON")
    void shouldDropUnparseableMessage() {
        var message = messageFrom(RabbitMQConfig.EXTRACTION_DLQ, "not-json");

        assertThatCode(() -> listener.onMessage(message)).doesNotThrowAnyException();
        verify(recordPipelineErrorUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should swallow InvalidStatusTransitionException as an idempotent no-op on duplicate delivery")
    void shouldSwallowInvalidStatusTransitionException() {
        var message = messageFrom(
                RabbitMQConfig.SCORING_DLQ,
                """
                {"analysisId": 1, "stage": "SCORING", "errorMessage": "boom", "stackTrace": "trace"}
                """
        );
        doThrow(new InvalidStatusTransitionException("already failed"))
                .when(recordPipelineErrorUseCase).execute(any());

        assertThatCode(() -> listener.onMessage(message)).doesNotThrowAnyException();
    }

    private Message messageFrom(String consumerQueue, String body) {
        MessageProperties properties = new MessageProperties();
        properties.setConsumerQueue(consumerQueue);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
