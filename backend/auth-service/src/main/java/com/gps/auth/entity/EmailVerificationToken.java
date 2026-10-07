package com.gps.auth.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="email_verification_tokens",
indexes={
        @Index(
                name="idx_email_verification_token",
                columnList = "user_id"
        ),
        @Index(
                name="idx_email_verification_token_expires_at",
                columnList = "expires_at"
        )
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(name="token_hash", nullable = false, unique = true,length = 64)
    private String tokenHash;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAT;

    @Column(name="used_at")
    private Instant usedAt;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate(){
        createdAt=Instant.now();
    }

    public boolean isUsed(){
        return usedAt!=null;
    }
    public  boolean isExpired(){
        return !expiresAT.isAfter(Instant.now());
    }

}
