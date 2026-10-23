
package com.gps.auth.controller;

import com.gps.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalAuthController {

    private static final Logger log =
            LoggerFactory.getLogger(InternalAuthController.class);

    private final AuthService authService;

    @GetMapping("/{publicId}/email")
    public ResponseEntity<Map<String, String>> getUserEmail(
            @PathVariable String publicId) {

        log.info(
                "[INTERNAL-AUTH] Controller reached | publicId={}",
                publicId
        );

        String email = authService.getEmailByPublicId(publicId);

        log.info(
                "[INTERNAL-AUTHENTIACTION] Email lookup successful | publicId={}",
                publicId
        );

        return ResponseEntity.ok(Map.of("email", email));
    }
}
