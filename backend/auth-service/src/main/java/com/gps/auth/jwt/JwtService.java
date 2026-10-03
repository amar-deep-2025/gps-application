package com.gps.auth.jwt;
import com.gps.auth.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;

    public String generateAccessToken(String publicId, String email) {
        return jwtUtil.generateAccessToken(publicId, email);
    }
}