package com.gps.notification.controller;

import com.gps.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping
    public String sendTestEmail(@RequestParam String to){
        emailService.sendEmail(
                to,
                "GPS Notification Test",
                "Congratulation! Gps Notifications Service email setup is working."
        );
        return "Test email sent successfully";
    }
}
