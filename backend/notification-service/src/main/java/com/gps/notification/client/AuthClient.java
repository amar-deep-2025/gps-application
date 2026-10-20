package com.gps.notification.client;

import com.gps.notification.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient.Builder restClientBuilder;
    private final JwtUtil jwtUtil;
    private Logger log= LoggerFactory.getLogger(AuthClient.class);
    @Value("${auth.service.base.url}")
    private String authServiceBaseUrl;


    public String getEmailByPublicId(UUID userPublicId){
        try{
            String serviceToken=jwtUtil.generateServiceToken();

            Map<?,?> response= restClientBuilder
                    .baseUrl(authServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/internal/users/{publicId}/email", userPublicId)
                    .header("Authorization","Bearer "+ serviceToken)
                    .retrieve()
                    .body(Map.class);

            if (response==null || response.get("email")==null){
                throw new IllegalStateException(
                        "Email not found in Auth Service response"
                );
            }
            log.info("email find={}",response.get("email"));
            return response.get("email").toString();

        }catch(Exception ex) {
            log.error("Failed to fetch user email from AuthService for userPublicId={}",
                    userPublicId,ex);
            throw ex;
        }

    }

}
