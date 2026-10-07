package com.gps.auth.service;


import com.gps.auth.entity.EmailVerificationToken;
import com.gps.auth.entity.User;
import com.gps.auth.repository.EmailVerificationTokenRepository;
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
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository
            emailVerificationTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${email-verification-token.expiration-ms}")
    private long expirationMs;

    @Transactional
    public String createToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setUser(user);
        verificationToken.setTokenHash(tokenHash);
        verificationToken.setExpiresAT(
                Instant.now().plusMillis(expirationMs)
        );

        emailVerificationTokenRepository.save(verificationToken);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public EmailVerificationToken validateToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid verification token"
                                ));

        if (verificationToken.isUsed()) {
            throw new IllegalArgumentException(
                    "Verification token has already been used"
            );
        }

        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException(
                    "Verification token has expired"
            );
        }

        return verificationToken;
    }

    @Transactional
    public void markAsUsed(
            EmailVerificationToken verificationToken) {

        verificationToken.setUsedAt(Instant.now());

        emailVerificationTokenRepository.save(verificationToken);
    }

    @Transactional
    public int invalidateActiveTokens(Long userId) {
        return emailVerificationTokenRepository.invalidateActiveTokens(
                userId,
                Instant.now()
        );
    }
    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

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

        StringBuilder result =
                new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}