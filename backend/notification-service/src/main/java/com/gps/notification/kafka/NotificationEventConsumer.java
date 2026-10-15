package com.gps.notification.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gps.notification.dto.event.NotificationEvent;
import com.gps.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "gps-notification-json-test")
    public void consume(String message) {
        try{
            NotificationEvent event=objectMapper.readValue(message, NotificationEvent.class);

            notificationService.processEvent(event);

            log.info("Notification event received : eventId={}, type={}, userId={}",
                    event.eventId(),event.type(), event.userId());


        }catch(JsonProcessingException exception){
            log.error("Invalid notification JSON. Raw message: {}",message, exception);
            throw new IllegalArgumentException("Invalid notification JSON", exception);
        }
    }
}
