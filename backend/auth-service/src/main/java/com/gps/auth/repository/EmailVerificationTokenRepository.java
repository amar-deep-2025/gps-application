package com.gps.auth.repository;

import com.gps.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE EmailVerificationToken evt
            SET evt.usedAt=:usedAt
            WHERE evt.user.id=:userId
            AND evt.usedAt is NULL
            """)
    int invalidateActiveTokens(@Param("userId") Long userId,
                               @Param("usedAt") Instant usedAt);
}
