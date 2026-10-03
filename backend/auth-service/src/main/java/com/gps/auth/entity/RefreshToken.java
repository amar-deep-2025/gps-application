package com.gps.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="refresh_tokens",
indexes={
        @Index(name="idx_refresh_token_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="revoked_at")
    private Instant revokedAt;


    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt=Instant.now();
    }

    public boolean isRevoked(){
        return revokedAt!=null;
    }

    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }

}
