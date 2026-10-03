
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

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){

        String path=request.getServletPath();
        return request.getMethod().equals("POST")
                &&(
                        path.equals("/api/auth/register")||
                                path.equals("/api/auth/login")||
                                path.startsWith("api/auth/refresh")||
                                path.startsWith("api/auth/logout")
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

        User user = userRepository.findByPublicId(publicId)
                .orElse(null);

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
                        user.getPublicId(),
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
