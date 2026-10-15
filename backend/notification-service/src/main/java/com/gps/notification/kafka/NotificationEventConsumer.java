package com.gps.notification.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationEventConsumer.class);

    @KafkaListener(topics = "gps-notification-events")
    public void consume(String message) {
        log.info("Kafka test message received successfully");
        log.info("Message: {}", message);
    }
}
