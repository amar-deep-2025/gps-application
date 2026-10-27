
package com.gps.notification.dto.request;

import com.gps.notification.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationRequest(
        Long id,
        UUID devicePublicId,
        String title,
        String message,
        NotificationType type,
        boolean isRead,
        Instant createdAt
) {
}
