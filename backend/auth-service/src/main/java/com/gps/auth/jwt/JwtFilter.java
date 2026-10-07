package com.gps.auth.jwt;

import com.gps.auth.entity.User;
import com.gps.auth.enums.Status;
import com.gps.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return "POST".equalsIgnoreCase(request.getMethod())
                && (
                path.equals("/api/auth/register")
                        || path.equals("/api/auth/login")
                        || path.equals("/api/auth/refresh")
                        || path.equals("/api/auth/logout")
                        || path.equals("/api/auth/forgot-password")
                        || path.equals("/api/auth/reset-password")
                        || path.startsWith("/api/auth/verify-email")
                        || path.startsWith("/api/auth/resend-verification")
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isAccessTokenValid(token)) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired access token"
            );
            return;
        }

        Claims claims = jwtUtil.extractClaims(token);
        String publicId = claims.getSubject();

        UUID userPublicId;

        try {
            userPublicId = UUID.fromString(publicId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid user identifier in token"
            );
            return;
        }

        User user = userRepository.findByPublicId(userPublicId)
                .orElse(null);
        System.out.println("JWT user found: "+(user!=null));
        if (user!=null){
            System.out.println("JWT user status: "+user.getStatus());
            System.out.println("JWT user role: "+user.getRole());
        }
        if (user == null) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "User not found"
            );
            return;
        }

        if (user.getStatus() != Status.ACTIVE) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "User account is not active"
            );
            return;
        }

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getPublicId().toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole().name()
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}