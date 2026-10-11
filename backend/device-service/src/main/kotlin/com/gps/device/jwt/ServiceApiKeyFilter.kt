
package com.gps.device.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ServiceApiKeyFilter(
    @Value("\${gps.service.api-key}")
    private val expectedApiKey: String
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI != "/rpc"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val providedApiKey = request.getHeader("X-Service-API-Key")

        if (providedApiKey.isNullOrBlank() ||
            providedApiKey != expectedApiKey
        ) {
            response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Invalid service API key"
            )
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(
            "gps-tracking-service",
            null,
            AuthorityUtils.createAuthorityList("ROLE_SERVICE")
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)

        filterChain.doFilter(request, response)
    }
}
