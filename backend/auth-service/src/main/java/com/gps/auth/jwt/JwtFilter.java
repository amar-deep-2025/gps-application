
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log =
            LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        boolean skip =
                "POST".equalsIgnoreCase(request.getMethod())
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

        log.debug(
                "[JWT-FILTER] shouldNotFilter | method={} | path={} | skip={}",
                request.getMethod(), path, skip
        );

        return skip;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        log.info(
                "[JWT-FILTER] START | method={} | path={} | uri={} | contextPath={} | dispatcher={}",
                method,
                path,
                request.getRequestURI(),
                request.getContextPath(),
                request.getDispatcherType()
        );

        try {
            boolean internalRoute =
                    "GET".equalsIgnoreCase(method)
                            && path.matches("/internal/users/[^/]+/email");

            log.info(
                    "[JWT-FILTER] Route classification | internalEmailRoute={}",
                    internalRoute
            );

            // Service-to-service authentication
            if (internalRoute) {
                String header = request.getHeader("Authorization");

                log.info(
                        "[JWT-FILTER] Internal auth header | present={} | bearerPrefix={}",
                        header != null,
                        header != null && header.startsWith("Bearer ")
                );

                if (header == null || !header.startsWith("Bearer ")) {
                    log.warn("[JWT-FILTER] DENY | reason=SERVICE_TOKEN_MISSING");
                    response.sendError(401, "Service token is required");
                    return;
                }

                String token = header.substring(7);
                boolean valid;

                try {
                    valid = jwtUtil.isServiceTokenValid(token);
                    log.info(
                            "[JWT-FILTER] Service JWT validation completed | valid={}",
                            valid
                    );
                } catch (Exception ex) {
                    log.error(
                            "[JWT-FILTER] Service JWT validation EXCEPTION | type={}",
                            ex.getClass().getSimpleName(), ex
                    );
                    valid = false;
                }

                if (!valid) {
                    log.warn("[JWT-FILTER] DENY | reason=INVALID_SERVICE_TOKEN");
                    response.sendError(401, "Invalid service token");
                    return;
                }

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                "notification-service",
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_INTERNAL_SERVICE"))
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                log.info(
                        "[JWT-FILTER] Service authentication set | principal={} | authorities={}",
                        authentication.getPrincipal(),
                        authentication.getAuthorities()
                );

                try {
                    log.info("[JWT-FILTER] Continuing filter chain for internal route");
                    filterChain.doFilter(request, response);
                } catch (Exception ex) {
                    log.error(
                            "[JWT-FILTER] Downstream exception on internal route | type={}",
                            ex.getClass().getSimpleName(), ex
                    );
                    throw ex;
                } finally {
                    log.info(
                            "[JWT-FILTER] END internal route | status={} | committed={}",
                            response.getStatus(), response.isCommitted()
                    );
                }
                return;
            }

            // Normal user access-token authentication
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                log.debug(
                        "[JWT-FILTER] No bearer token; continuing chain | path={}",
                        path
                );
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7);
            boolean validAccessToken;

            try {
                validAccessToken = jwtUtil.isAccessTokenValid(token);
                log.info(
                        "[JWT-FILTER] Access JWT validation | valid={}",
                        validAccessToken
                );
            } catch (Exception ex) {
                log.error(
                        "[JWT-FILTER] Access JWT validation EXCEPTION | type={}",
                        ex.getClass().getSimpleName(), ex
                );
                validAccessToken = false;
            }

            if (!validAccessToken) {
                log.warn("[JWT-FILTER] DENY | reason=INVALID_ACCESS_TOKEN");
                SecurityContextHolder.clearContext();
                response.sendError(401, "Invalid or expired access token");
                return;
            }

            Claims claims = jwtUtil.extractClaims(token);
            String subject = claims.getSubject();

            log.debug(
                    "[JWT-FILTER] Access token claims extracted | subjectPresent={}",
                    subject != null && !subject.isBlank()
            );

            UUID userPublicId;
            try {
                userPublicId = UUID.fromString(subject);
            } catch (IllegalArgumentException | NullPointerException ex) {
                log.warn(
                        "[JWT-FILTER] DENY | reason=INVALID_USER_ID_IN_TOKEN"
                );
                SecurityContextHolder.clearContext();
                response.sendError(401, "Invalid user identifier in token");
                return;
            }

            log.info("[JWT-FILTER] Looking up user by public ID");

            User user = userRepository.findByPublicId(userPublicId)
                    .orElse(null);

            log.info(
                    "[JWT-FILTER] User lookup result | found={}",
                    user != null
            );

            if (user == null) {
                log.warn("[JWT-FILTER] DENY | reason=USER_NOT_FOUND");
                SecurityContextHolder.clearContext();
                response.sendError(401, "User not found");
                return;
            }

            log.info(
                    "[JWT-FILTER] User eligibility | status={} | role={}",
                    user.getStatus(), user.getRole()
            );

            if (user.getStatus() != Status.ACTIVE) {
                log.warn("[JWT-FILTER] DENY | reason=USER_NOT_ACTIVE");
                SecurityContextHolder.clearContext();
                response.sendError(403, "User account is not active");
                return;
            }

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getPublicId().toString(),
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + user.getRole().name()))
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            log.info(
                    "[JWT-FILTER] User authentication set | roleAuthority=ROLE_{}",
                    user.getRole().name()
            );

            try {
                filterChain.doFilter(request, response);
            } finally {
                log.info(
                        "[JWT-FILTER] END user route | status={} | committed={}",
                        response.getStatus(), response.isCommitted()
                );
            }

        } catch (IOException | ServletException ex) {
            log.error(
                    "[JWT-FILTER] Request processing exception | path={} | type={}",
                    path, ex.getClass().getSimpleName(), ex
            );
            throw ex;
        } catch (RuntimeException ex) {
            log.error(
                    "[JWT-FILTER] Unexpected runtime exception | path={} | type={}",
                    path, ex.getClass().getSimpleName(), ex
            );
            throw ex;
        }
    }
}
