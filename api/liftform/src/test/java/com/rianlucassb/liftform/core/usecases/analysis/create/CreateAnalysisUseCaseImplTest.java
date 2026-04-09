package com.rianlucassb.liftform.core.usecases.analysis.create;

import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CreateAnalysisUseCaseImplTest {

    @Mock
    private VideoAnalysisRepository videoAnalysisRepository;

    @InjectMocks
    private CreateAnalysisUseCaseImpl createAnalysisUseCase;

    @Test
    @DisplayName("Should return an uploadURL and an analysisID when execute is called with valid input")
    void shouldReturnUploadURLAndAnalysisIDWhenInputIsValid() {
        // Arrange
        var input = createValidInput();

        doReturn(createValidVideoAnalysis()).when(videoAnalysisRepository).save(org.mockito.ArgumentMatchers.any());

        // Act
        var output = createAnalysisUseCase.execute(input);

        // Assert
        assertThat(output.analysisId()).isNotNull();
        assertThat(output.uploadUrl()).isNotNull();
        assertThat(output.expiresIn()).isGreaterThan(0);
    }

    private VideoAnalysis createValidVideoAnalysis() {
        var userId = UUID.randomUUID();
        return new VideoAnalysis(
                1L,
                userId,
                "SQUAT",
                "videos/" + userId + "/" + UUID.randomUUID(),
                null,
                null,
                null
        );
    }


    private CreateAnalysisUseCaseInput createValidInput() {
        return new CreateAnalysisUseCaseInput(
                UUID.randomUUID().toString(),
                "SQUAT",
                "filename.mp4"
        );
    }
}