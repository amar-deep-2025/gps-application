
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

            println("JWT subject: $publicId")
            println("JWT type: $type")

            type == "access" && !publicId.isNullOrBlank()
        } catch (ex: JwtException) {
            println("JWT exception: ${ex.javaClass.simpleName}: ${ex.message}")
            false
        } catch (ex: IllegalArgumentException) {
            println("Argument exception: ${ex.message}")
            false
        }
    }
    fun extractPublicId(token: String): String {
        return extractClaims(token).subject
    }
}
