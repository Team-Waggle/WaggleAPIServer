package io.waggle.waggleapiserver.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.waggle.waggleapiserver.common.util.logger
import io.waggle.waggleapiserver.domain.user.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.access-token-ttl}") private val accessTokenTtl: Long,
    @Value("\${jwt.refresh-token-ttl}") private val refreshTokenTtl: Long,
) {
    private val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateAccessToken(
        userId: UUID,
        role: UserRole,
    ): String = generateToken(userId, role, accessTokenTtl)

    fun generateRefreshToken(
        userId: UUID,
        role: UserRole,
    ): String = generateToken(userId, role, refreshTokenTtl)

    fun isTokenValid(token: String): Boolean =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: SecurityException) {
            logger.warn("Invalid JWT signature: ${e.message}", e)
            false
        } catch (e: MalformedJwtException) {
            logger.debug("Malformed JWT token: ${e.message}")
            false
        } catch (e: ExpiredJwtException) {
            logger.debug("Expired JWT token")
            false
        } catch (e: UnsupportedJwtException) {
            logger.debug("Unsupported JWT token: ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            logger.debug("Empty JWT claims string: ${e.message}")
            false
        }

    fun getUserIdFromToken(token: String): UUID = UUID.fromString(getClaimsFromToken(token).subject)

    fun getRoleFromToken(token: String): UserRole =
        (getClaimsFromToken(token)["role"] as? String)
            ?.let { UserRole.valueOf(it) }
            ?: UserRole.USER

    private fun generateToken(
        userId: UUID,
        role: UserRole,
        ttl: Long,
    ): String {
        val now = Date()
        val expiration = Date(now.time + ttl)

        return Jwts
            .builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }

    private fun getClaimsFromToken(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
