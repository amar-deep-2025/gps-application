package com.gps.notification.repository;

import com.gps.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(
            UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    boolean existsByEventId(UUID eventId);
}