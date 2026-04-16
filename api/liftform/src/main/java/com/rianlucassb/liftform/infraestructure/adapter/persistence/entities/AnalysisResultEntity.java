package com.rianlucassb.liftform.infraestructure.adapter.persistence.entities;

import com.rianlucassb.liftform.core.domain.model.enums.Classification;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "TB_ANALYSIS_RESULTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANALYSIS_ID", nullable = false, unique = true)
    private VideoAnalysisEntity analysis;

    @Column(name = "MODEL_VERSION", nullable = false, length = 20)
    private String modelVersion;

    @Column(name = "OVERALL_SCORE", nullable = false, precision = 5, scale = 4)
    private BigDecimal overallScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASSIFICATION", nullable = false, length = 20)
    private Classification classification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "FEEDBACK", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> feedback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "RAW_FEATURES", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawFeatures;

    @Column(name = "SCORED_AT", nullable = false)
    private Instant scoredAt;

    @PrePersist
    private void prePersist() {
        this.scoredAt = Instant.now();
    }
}