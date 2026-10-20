
package com.gps.auth.config;

import com.gps.auth.jwt.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("[SECURITY-CONFIG] Creating BCrypt PasswordEncoder");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http
    ) throws Exception {

        log.info("[SECURITY-CONFIG] Building SecurityFilterChain");

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/internal/users/*/email"
                        ).hasAuthority("ROLE_INTERNAL_SERVICE")
                        .requestMatchers("/error").permitAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exceptions -> exceptions

                        .authenticationEntryPoint((request, response, ex) -> {
                            log.error(
                                    "[SECURITY-DIAGNOSTIC] AUTHENTICATION DENIED | method={} | path={} | exception={} | message={}",
                                    request.getMethod(),
                                    request.getServletPath(),
                                    ex.getClass().getSimpleName(),
                                    ex.getMessage()
                            );

                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "Authentication required"
                            );
                        })

                        .accessDeniedHandler((request, response, ex) -> {
                            Authentication authentication =
                                    SecurityContextHolder.getContext()
                                            .getAuthentication();

                            log.error(
                                    "[SECURITY-DIAGNOSTIC] AUTHORIZATION DENIED | method={} | path={} | principal={} | authorities={} | authenticated={} | exception={} | message={}",
                                    request.getMethod(),
                                    request.getServletPath(),
                                    authentication != null
                                            ? authentication.getName() : "anonymous",
                                    authentication != null
                                            ? authentication.getAuthorities() : "none",
                                    authentication != null
                                            && authentication.isAuthenticated(),
                                    ex.getClass().getSimpleName(),
                                    ex.getMessage()
                            );

                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "Access denied"
                            );
                        })
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        log.info("[SECURITY-CONFIG] SecurityFilterChain configured successfully");

        return http.build();
    }
}
