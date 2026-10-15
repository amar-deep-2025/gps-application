
package com.gps.notification.dto.response;

import java.util.List;

public record NotificationPageResponse(
        List<com.gps.notification.dto.response.NotificationResponse> notifications,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
