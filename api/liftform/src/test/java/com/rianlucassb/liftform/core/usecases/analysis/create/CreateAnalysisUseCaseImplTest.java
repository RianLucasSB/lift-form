package com.rianlucassb.liftform.core.usecases.analysis.create;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CreateAnalysisUseCaseImplTest {

    @InjectMocks
    private CreateAnalysisUseCaseImpl createAnalysisUseCase;

    @Test
    @DisplayName("Should return an uploadURL and an analysisID when execute is called with valid input")
    void shouldReturnUploadURLAndAnalysisIDWhenInputIsValid() {
        // Arrange
        var input = createValidInput();

        // Act
        var output = createAnalysisUseCase.execute(input);

        // Assert
        assertThat(output.analysisId()).isNotNull();
        assertThat(output.uploadUrl()).isNotNull();
        assertThat(output.expiresIn()).isGreaterThan(0);
    }


    private CreateAnalysisUseCaseInput createValidInput() {
        return new CreateAnalysisUseCaseInput(
                UUID.randomUUID().toString(),
                "validUserID",
                "filename.mp4"
        );
    }
}