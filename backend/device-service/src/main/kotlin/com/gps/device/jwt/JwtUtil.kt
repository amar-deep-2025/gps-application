
package com.gps.device.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtil(
    @Value("\${jwt.access.secret}") secret: String
) {
    private val secretKey = Keys.hmacShaKeyFor(
        Decoders.BASE64.decode(secret)
    )

    fun extractClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun isAccessTokenValid(token: String): Boolean {
        return try {
            val claims = extractClaims(token)
            val publicId = claims.subject
            val type = claims["type", String::class.java]

            type == "access" && !publicId.isNullOrBlank()
        } catch (ex: JwtException) {
            false
        } catch (ex: IllegalArgumentException) {
            false
        }
    }

    fun extractPublicId(token: String): String {
        return extractClaims(token).subject
    }
}
