package com.gps.auth.dto.response;

public record TokenResponse(
        String message,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
