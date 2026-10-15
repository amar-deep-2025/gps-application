package com.gps.notification.dto.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        Long userId,
        UUID devicePublicId,
        String type,
        String title,
        String message,
        List<String> channels,
        Map<String, String> metadata
) {
}
