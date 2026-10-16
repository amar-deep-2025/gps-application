package com.gps.notification.service;

import com.gps.notification.dto.event.NotificationEvent;
import com.gps.notification.entity.Notification;
import com.gps.notification.enums.NotificationType;
import com.gps.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void processEvent(NotificationEvent event){

        if (event.userId()==null || event.type()==null
                || event.title()==null || event.message()==null)
        {
            throw new IllegalArgumentException("Notification event is missing required fields");
        }

        if (notificationRepository.existsByEventId(event.eventId())){
            return;
        }

        Notification notification=new Notification();
        notification.setUserId(event.userId());
        notification.setDevicePublicId(event.devicePublicId());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setType(NotificationType.valueOf(event.type()));
        notification.setEventId(event.eventId());
        try{
            notificationRepository.saveAndFlush(notification);
        }catch(DataIntegrityViolationException exception){
            if (notificationRepository.existsByEventId(event.eventId())){
                return;
            }
            throw exception;
        }



    }
}
