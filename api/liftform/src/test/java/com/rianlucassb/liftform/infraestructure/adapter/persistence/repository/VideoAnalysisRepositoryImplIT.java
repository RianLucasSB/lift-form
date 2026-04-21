package com.rianlucassb.liftform.infraestructure.adapter.persistence.repository;

import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.domain.model.enums.ExerciseType;
import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("VideoAnalysisRepository Integration Tests")
class VideoAnalysisRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private VideoAnalysisRepository videoAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(new User(null, "analyst", "analyst@example.com", "pwd", null));
    }

    @Test
    @DisplayName("save() persists a VideoAnalysis and returns it with generated id")
    void save_persistsVideoAnalysis() {
        VideoAnalysis analysis = new VideoAnalysis(
                null,
                savedUser.id(),
                ExerciseType.SQUAT,
                "videos/SQUAT/" + savedUser.id() + "/test.mp4",
                VideoAnalysisStatus.CREATED,
                null,
                null
        );

        VideoAnalysis saved = videoAnalysisRepository.save(analysis);

        assertThat(saved.id()).isNotNull().isPositive();
        assertThat(saved.userId()).isEqualTo(savedUser.id());
        assertThat(saved.exerciseType()).isEqualTo(ExerciseType.SQUAT);
        assertThat(saved.videoS3Key()).isEqualTo("videos/SQUAT/" + savedUser.id() + "/test.mp4");
    }

    @Test
    @DisplayName("save() sets status to CREATED (not overridden by @PrePersist)")
    void save_statusIsCREATED_whenExplicitlySet() {
        VideoAnalysis analysis = new VideoAnalysis(
                null, savedUser.id(), ExerciseType.SQUAT,
                "videos/SQUAT/" + savedUser.id() + "/v1.mp4",
                VideoAnalysisStatus.CREATED, null, null
        );

        VideoAnalysis saved = videoAnalysisRepository.save(analysis);

        assertThat(saved.status()).isEqualTo(VideoAnalysisStatus.CREATED);
    }

    @Test
    @DisplayName("save() defaults status to CREATED when status is null via @PrePersist")
    void save_statusDefaultsToCREATED_whenNull() {
        // Pass null status — @PrePersist should default to CREATED
        VideoAnalysis analysis = new VideoAnalysis(
                null, savedUser.id(), ExerciseType.SQUAT,
                "videos/SQUAT/" + savedUser.id() + "/v2.mp4",
                null, null, null
        );

        VideoAnalysis saved = videoAnalysisRepository.save(analysis);

        assertThat(saved.status()).isEqualTo(VideoAnalysisStatus.CREATED);
    }

    @Test
    @DisplayName("save() sets createdAt and updatedAt timestamps via @PrePersist")
    void save_setsTimestamps() {
        VideoAnalysis analysis = new VideoAnalysis(
                null, savedUser.id(), ExerciseType.SQUAT,
                "videos/SQUAT/" + savedUser.id() + "/v3.mp4",
                VideoAnalysisStatus.CREATED, null, null
        );

        VideoAnalysis saved = videoAnalysisRepository.save(analysis);

        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.updatedAt()).isNotNull();
    }
}

