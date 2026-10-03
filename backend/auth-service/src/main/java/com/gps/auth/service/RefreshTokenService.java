package com.gps.auth.service;

import com.gps.auth.dto.response.TokenResponse;
import com.gps.auth.entity.RefreshToken;
import com.gps.auth.entity.User;
import com.gps.auth.enums.Status;
import com.gps.auth.jwt.JwtService;
import com.gps.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    private final SecureRandom secureRandom=new SecureRandom();

    @Value("${refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String createRefreshToken(User user){
        byte[] randomBytes=new byte[32];

        secureRandom.nextBytes(randomBytes);

        String rawToken= Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
        RefreshToken refreshToken=new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);
        return  rawToken;
    }

    @Transactional
    public TokenResponse refreshTokens(String rawToken){

        RefreshToken oldToken=refreshTokenRepository.findByTokenHash(hashToken(rawToken)).orElseThrow(()->new IllegalArgumentException(
                "Invalid refresh token"
        ));

        if (oldToken.isRevoked() || oldToken.isExpired()){
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        User user=oldToken.getUser();
        if (user.getStatus()!= Status.ACTIVE){
            throw new IllegalArgumentException("User account is not active");
        }

        oldToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(oldToken);

        String newAccessToken=jwtService.generateAccessToken(user.getPublicId().toString(),user.getEmail());
        String newRefreshToken=createRefreshToken(user);

        return new TokenResponse(
                "Tokens refreshed successfully",
                newAccessToken,
                newRefreshToken,
                "Bearer"
        );

    }

    @Transactional
    public void logout(String rawToken){
        if (rawToken==null || rawToken.isBlank()){
            return;
        }
        String tokenHash=hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token->{
            if(!token.isRevoked()){
                token.setRevokedAt(Instant.now());
            }
        });
    }
    public String hashToken(String token){
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");

            byte[] hash=digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        }catch(Exception e){
            throw new IllegalStateException("Unable to hash refresh token",e);
        }
    }

    @Transactional
    public int logoutAll(Long userId){
        return refreshTokenRepository.revokeAllActiveTokens(
                userId,
                Instant.now()
        );
    }
}
