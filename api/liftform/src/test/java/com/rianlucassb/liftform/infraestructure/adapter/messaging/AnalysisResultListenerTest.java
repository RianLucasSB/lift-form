package com.rianlucassb.liftform.infraestructure.adapter.messaging;

import com.rianlucassb.liftform.core.domain.exception.InvalidStatusTransitionException;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCase;
import com.rianlucassb.liftform.core.usecases.analysis.recordresult.RecordAnalysisResultUseCaseInput;
import com.rianlucassb.liftform.infraestructure.adapter.messaging.dto.AnalysisFinishedMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisResultListenerTest {

    @Mock private RecordAnalysisResultUseCase recordAnalysisResultUseCase;

    @InjectMocks
    private AnalysisResultListener listener;

    @Test
    @DisplayName("Should map the incoming message to the use case input and execute it")
    void shouldMapMessageAndExecuteUseCase() {
        var message = new AnalysisFinishedMessage(
                1L, "squat-v1", new BigDecimal("0.82"),
                Map.of("depth", "good"), Map.of("knee_bottom_avg", 48.0)
        );

        listener.onMessage(message);

        ArgumentCaptor<RecordAnalysisResultUseCaseInput> captor = ArgumentCaptor.forClass(RecordAnalysisResultUseCaseInput.class);
        verify(recordAnalysisResultUseCase).execute(captor.capture());

        assertThat(captor.getValue().videoAnalysisId()).isEqualTo(1L);
        assertThat(captor.getValue().modelVersion()).isEqualTo("squat-v1");
        assertThat(captor.getValue().overallScore()).isEqualByComparingTo("0.82");
        assertThat(captor.getValue().feedback()).isEqualTo(Map.of("depth", "good"));
        assertThat(captor.getValue().rawFeatures()).isEqualTo(Map.of("knee_bottom_avg", 48.0));
    }

    @Test
    @DisplayName("Should swallow InvalidStatusTransitionException as an idempotent no-op on duplicate delivery")
    void shouldSwallowInvalidStatusTransitionException() {
        var message = new AnalysisFinishedMessage(1L, "squat-v1", new BigDecimal("0.82"), Map.of(), Map.of());
        doThrow(new InvalidStatusTransitionException("already completed"))
                .when(recordAnalysisResultUseCase).execute(any());

        assertThatCode(() -> listener.onMessage(message)).doesNotThrowAnyException();
    }
}
