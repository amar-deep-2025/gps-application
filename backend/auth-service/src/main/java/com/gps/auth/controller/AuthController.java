package com.gps.auth.controller;


import com.gps.auth.dto.request.LoginRequest;
import com.gps.auth.dto.request.RefreshTokenRequest;
import com.gps.auth.dto.request.RegisterRequest;
import com.gps.auth.dto.response.LoginResponse;
import com.gps.auth.dto.response.TokenResponse;
import com.gps.auth.service.AuthService;
import com.gps.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody
                                                            RegisterRequest request){
        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message","user registered successfully"));


    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){

        LoginResponse response=authService.login(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){
        TokenResponse response=refreshTokenService.refreshTokens(request.refreshToken());
        return  ResponseEntity.ok(response);
    }
}
