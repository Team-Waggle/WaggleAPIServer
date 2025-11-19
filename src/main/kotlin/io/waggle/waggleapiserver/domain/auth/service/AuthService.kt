package io.waggle.waggleapiserver.domain.auth.service

import io.waggle.waggleapiserver.domain.auth.AuthCookieManager
import io.waggle.waggleapiserver.domain.auth.dto.response.AccessTokenResponse
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.jwt.JwtProvider
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class AuthService(
    private val authCookieManager: AuthCookieManager,
    private val jwtProvider: JwtProvider,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${jwt.refresh-token-ttl}") private val refreshTokenTtl: Long,
) {
    fun issueTokens(
        userId: UUID,
        role: UserRole,
        response: HttpServletResponse,
    ): String {
        val accessToken = jwtProvider.generateAccessToken(userId, role)
        val refreshToken = jwtProvider.generateRefreshToken(userId, role)

        redisTemplate.opsForValue().set(
            "refresh-token:$userId",
            refreshToken,
            Duration.ofMillis(refreshTokenTtl),
        )

        val maxAgeSeconds = (refreshTokenTtl / 1000).toInt()
        authCookieManager.addRefreshTokenCookie(response, refreshToken, maxAgeSeconds)

        return accessToken
    }

    fun refresh(refreshToken: String): AccessTokenResponse {
        require(jwtProvider.isTokenValid(refreshToken)) { "Invalid refresh token" }

        val userId = jwtProvider.getUserIdFromToken(refreshToken)
        val role = jwtProvider.getRoleFromToken(refreshToken)

        val stored =
            redisTemplate.opsForValue().get("refresh-token:$userId")
                ?: throw IllegalArgumentException("Refresh token not found")

        require(stored == refreshToken) { "Refresh token mismatch" }

        return AccessTokenResponse(jwtProvider.generateAccessToken(userId, role))
    }

    fun logout(
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        if (jwtProvider.isTokenValid(refreshToken)) {
            val userId = jwtProvider.getUserIdFromToken(refreshToken)
            redisTemplate.delete("refresh-token:$userId")
        }

        authCookieManager.expireRefreshTokenCookie(response)
    }
}
