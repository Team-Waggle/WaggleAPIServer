package io.waggle.waggleapiserver.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
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
    ): String = generateToken(userId, role, TokenType.ACCESS, accessTokenTtl)

    fun generateRefreshToken(
        userId: UUID,
        role: UserRole,
        sessionId: UUID,
    ): String = generateToken(userId, role, TokenType.REFRESH, refreshTokenTtl, sessionId)

    fun isTokenValid(
        token: String,
        type: TokenType,
    ): Boolean =
        try {
            getClaimsFromToken(token)[TYPE_CLAIM] == type.claimValue
        } catch (e: ExpiredJwtException) {
            logger.debug("Expired JWT token")
            false
        } catch (e: JwtException) {
            logger.warn("Invalid JWT (${e.javaClass.simpleName}): ${e.message}")
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

    fun getSessionIdFromToken(token: String): UUID =
        UUID.fromString(getClaimsFromToken(token)[SESSION_ID_CLAIM] as String)

    private fun generateToken(
        userId: UUID,
        role: UserRole,
        type: TokenType,
        ttl: Long,
        sessionId: UUID? = null,
    ): String {
        val now = Date()
        val expiration = Date(now.time + ttl)

        // iat, exp가 초 단위라 같은 초에 갱신하면 이전과 같은 토큰이 나와 재사용 탐지가 구분하지 못함
        return Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .claim("role", role.name)
            .claim(TYPE_CLAIM, type.claimValue)
            .apply { sessionId?.let { claim(SESSION_ID_CLAIM, it.toString()) } }
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

    companion object {
        private const val TYPE_CLAIM = "typ"
        private const val SESSION_ID_CLAIM = "sid"
    }
}
