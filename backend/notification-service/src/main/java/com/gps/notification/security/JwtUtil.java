
package com.gps.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
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
                    && claims.get("email", String.class) != null
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

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

    public String generateServiceToken() {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject("notification-service")
                .claim("type", "service")
                .claim("service", "notification-service")
                .claim("target", "auth-service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(120_000)))
                .signWith(getSecretKey())
                .compact();
    }

    public boolean isServiceTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);

            return "service".equals(
                    claims.get("type", String.class))
                    && "notification-service".equals(
                    claims.get("service", String.class))
                    && "notification-service".equals(
                    claims.getSubject())
                    && "auth-service".equals(
                    claims.get("target", String.class))
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
