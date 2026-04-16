package com.rianlucassb.liftform.infraestructure.adapter.persistence.entities;

import com.rianlucassb.liftform.core.domain.model.enums.PipelineStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TB_PIPELINE_ERRORS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANALYSIS_ID", nullable = false)
    private VideoAnalysisEntity analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "STAGE", nullable = false, length = 30)
    private PipelineStage stage;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "STACK_TRACE")
    private String stackTrace;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    @PrePersist
    private void prePersist() {
        this.occurredAt = Instant.now();
    }
}