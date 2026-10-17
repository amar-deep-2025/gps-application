package com.gps.notification.dto.response;

import com.gps.notification.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        Long id,
        UUID devicePublicId,
        String title,
        String message,
        NotificationType type,
        boolean isRead,
        Instant createdAt
) {
}
