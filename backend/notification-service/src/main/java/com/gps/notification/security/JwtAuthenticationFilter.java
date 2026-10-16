
package com.gps.notification.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (!jwtUtil.isAccessTokenValid(token)) {
                SecurityContextHolder.clearContext();
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired access token"
                );
                return;
            }

            String userPublicId = jwtUtil.extractPublicId(token);
            String email = jwtUtil.extractEmail(token);

            Map<String, String> principal = Map.of(
                    "userPublicId", userPublicId,
                    "email", email
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            AuthorityUtils.NO_AUTHORITIES
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
