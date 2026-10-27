
package com.gps.notification.service;

import com.gps.notification.client.AuthClient;
import com.gps.notification.dto.event.NotificationEvent;
import com.gps.notification.entity.Notification;
import com.gps.notification.enums.NotificationType;
import com.gps.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthClient authClient;
    private final EmailService emailService;
    private final Logger log= LoggerFactory.getLogger(NotificationService.class);

    public void processEvent(NotificationEvent event) {

        if (event == null
                || event.eventId() == null
                || event.userId() == null
                || event.type() == null
                || event.title() == null
                || event.message() == null) {

            throw new IllegalArgumentException(
                    "Notification event is missing required fields");
        }
        log.info("Saving notification eventId={}", event.eventId());
        if (notificationRepository.existsByEventId(event.eventId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setEventId(event.eventId());
        notification.setUserId(event.userId());
        notification.setDevicePublicId(event.devicePublicId());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setType(NotificationType.valueOf(event.type()));

        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException exception) {
            throw exception;
        }
        log.info("Event channels received:{}", event.channels());
        if (event.channels() != null
                && event.channels().contains("EMAIL")) {
            log.info("Email channels matched for eventId={}", event.eventId());
            log.info("Calling AuthClient for userId={}", event.userId());
            String email = authClient.getEmailByPublicId(event.userId());
            log.info("Resolved recipients email: {}",email);

            emailService.sendEmail(
                    email,
                    event.title(),
                    event.message());
            log.info("email sent successfully");
        }else{
            log.warn("Email channel not required. channels={}", event.channels());
        }

    }
}
