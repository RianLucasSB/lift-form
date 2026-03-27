package com.rianlucassb.liftform.infraestructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "TB_REFRESH_TOKEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshTokenEntity {
    @Id
    @Column(name = "TOKEN_HASH", nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @Column(name = "REVOKED", nullable = false)
    private boolean revoked = false;
}
