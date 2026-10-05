package com.gps.auth.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID publicId,
        String name,
        String email,
        String phone,
        String role,
        String status,
        boolean emailVerified,
        boolean phoneVerified




) {
}
