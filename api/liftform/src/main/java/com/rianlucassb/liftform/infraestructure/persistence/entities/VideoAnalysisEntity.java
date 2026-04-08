package com.rianlucassb.liftform.infraestructure.persistence.entities;

import com.rianlucassb.liftform.core.domain.model.enums.VideoAnalysisStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "TB_VIDEO_ANALYSIS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "EXERCISE_TYPE", nullable = false, length = 50)
    private String exerciseType;

    @Column(name = "VIDEO_S3_KEY", nullable = false)
    private String videoS3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private VideoAnalysisStatus status;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "analysis", fetch = FetchType.LAZY)
    private AnalysisResultEntity result;

    @OneToMany(mappedBy = "analysis", fetch = FetchType.LAZY)
    private List<PipelineErrorEntity> errors;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = VideoAnalysisStatus.UPLOADED;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }
}