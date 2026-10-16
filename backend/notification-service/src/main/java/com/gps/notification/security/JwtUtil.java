package com.gps.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.access.secret}")
    private String secret;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);

            return "access".equals(
                    claims.get("type", String.class))
                    && claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && claims.get("email", String.class) != null;

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractPublicId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }
}