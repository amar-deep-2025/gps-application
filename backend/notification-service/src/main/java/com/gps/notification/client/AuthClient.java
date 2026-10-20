
package com.gps.notification.client;

import com.gps.notification.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private static final Logger log =
            LoggerFactory.getLogger(AuthClient.class);

    private final RestClient.Builder restClientBuilder;
    private final JwtUtil jwtUtil;

    @Value("${auth.service.base.url}")
    private String authServiceBaseUrl;

    public String getEmailByPublicId(UUID userPublicId) {

        if (userPublicId == null) {
            throw new IllegalArgumentException(
                    "User public ID must not be null"
            );
        }

        try {
            String serviceToken = jwtUtil.generateServiceToken();

            Map<?, ?> response = restClientBuilder
                    .baseUrl(authServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/internal/users/{publicId}/email", userPublicId)
                    .header(
                            "Authorization",
                            "Bearer " + serviceToken
                    )
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("email") == null) {
                throw new IllegalStateException(
                        "Email missing from Auth Service response"
                );
            }

            return response.get("email").toString();

        } catch (RestClientResponseException ex) {
            log.error(
                    "Auth Service returned HTTP status {} for userPublicId={}",
                    ex.getStatusCode(),
                    userPublicId
            );
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "Failed to fetch email from Auth Service for userPublicId={}",
                    userPublicId,
                    ex
            );
            throw ex;
        }
    }
}
