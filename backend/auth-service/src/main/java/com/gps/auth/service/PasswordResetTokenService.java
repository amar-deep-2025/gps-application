package com.gps.auth.service;

import com.gps.auth.entity.PasswordResetToken;
import com.gps.auth.entity.User;
import com.gps.auth.repository.PasswordResetTokenRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${password-reset-token.expiration-ms}")
    private long expirationMs;

    private final SecureRandom secureRandom=new SecureRandom();

    @Transactional
    public String createToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(
                Instant.now().plusMillis(expirationMs)
        );

        passwordResetTokenRepository.save(resetToken);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException(
                    "Reset token has already been used"
            );
        }

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException(
                    "Reset token has expired"
            );
        }

        return resetToken;
    }

    @Transactional
    public void markAsUsed(PasswordResetToken resetToken) {

        resetToken.setUsedAt(Instant.now());

        passwordResetTokenRepository.save(resetToken);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder result = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }


}
