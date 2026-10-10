package com.gps.device.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() ||
            !authHeader.startsWith("Bearer ")
        ) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        try {
            if (!jwtUtil.isAccessTokenValid(token)) {
                response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid access token"
                )
                return
            }

            if (SecurityContextHolder.getContext().authentication == null) {
                val publicId = jwtUtil.extractPublicId(token)

                val authentication =
                    UsernamePasswordAuthenticationToken(
                        publicId,
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                    )

                val context =
                    SecurityContextHolder.createEmptyContext()

                context.authentication = authentication
                SecurityContextHolder.setContext(context)
            }
        } catch (ex: Exception) {
            SecurityContextHolder.clearContext()

            response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Invalid or expired access token"
            )
            return
        }

        filterChain.doFilter(request, response)
    }
}