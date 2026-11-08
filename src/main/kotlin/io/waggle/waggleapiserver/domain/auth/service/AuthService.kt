package io.waggle.waggleapiserver.domain.auth.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.auth.AuthCookieManager
import io.waggle.waggleapiserver.domain.auth.dto.response.AccessTokenResponse
import io.waggle.waggleapiserver.domain.auth.dto.response.WsTokenResponse
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.jwt.JwtProvider
import io.waggle.waggleapiserver.security.jwt.TokenType
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class AuthService(
    @Value("\${jwt.refresh-token-ttl}") private val refreshTokenTtl: Long,
    private val authCookieManager: AuthCookieManager,
    private val jwtProvider: JwtProvider,
    private val redisTemplate: StringRedisTemplate,
) {
    fun issueOttForOAuth(
        userId: UUID,
        role: UserRole,
    ): String {
        // 로그인마다 새 세션을 만들어 다른 기기의 세션을 덮어쓰지 않음
        val sessionId = UUID.randomUUID()
        val accessToken = jwtProvider.generateAccessToken(userId, role)
        val refreshToken = jwtProvider.generateRefreshToken(userId, role, sessionId)

        saveRefreshToken(userId, sessionId, refreshToken)

        val ott = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "oauth-ott:$ott",
            accessToken,
            Duration.ofMinutes(1),
        )
        // 쿠키를 여기서도 심으면 쿠키함이 둘로 갈려 한쪽의 refresh가 다른 쪽을 재사용 탐지로 끊음
        redisTemplate.opsForValue().set(
            "oauth-ott-refresh:$ott",
            refreshToken,
            Duration.ofMinutes(1),
        )

        return ott
    }

    fun redeemOtt(
        ott: String,
        response: HttpServletResponse,
    ): AccessTokenResponse {
        val accessToken =
            redisTemplate.opsForValue().getAndDelete("oauth-ott:$ott")
                ?: throw BusinessException(
                    ErrorCode.OAUTH_OTT_INVALID,
                    "Invalid or expired OAuth one-time token",
                )

        // 통과시키면 리프레시 없는 반쪽 로그인이 되어 액세스 토큰 만료 시 조용히 끊김
        val refreshToken =
            redisTemplate.opsForValue().getAndDelete("oauth-ott-refresh:$ott")
                ?: throw BusinessException(
                    ErrorCode.OAUTH_OTT_INVALID,
                    "OAuth one-time token has no paired refresh token",
                )

        val maxAgeSeconds = (refreshTokenTtl / 1000).toInt()
        authCookieManager.addRefreshTokenCookie(response, refreshToken, maxAgeSeconds)

        return AccessTokenResponse(accessToken)
    }

    fun refresh(
        refreshToken: String,
        response: HttpServletResponse,
    ): AccessTokenResponse {
        if (!jwtProvider.isTokenValid(refreshToken, TokenType.REFRESH)) {
            throw BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")
        }

        val userId = jwtProvider.getUserIdFromToken(refreshToken)
        val role = jwtProvider.getRoleFromToken(refreshToken)
        val sessionId = jwtProvider.getSessionIdFromToken(refreshToken)

        val stored =
            redisTemplate.opsForValue().get(refreshTokenKey(userId, sessionId))
                ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token not found")

        if (stored != refreshToken) {
            // 이미 교체된 토큰이 다시 쓰이면 탈취로 보고 이 세션만 끊음
            deleteRefreshToken(userId, sessionId)
            throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token reuse detected")
        }

        val newAccessToken = jwtProvider.generateAccessToken(userId, role)
        val newRefreshToken = jwtProvider.generateRefreshToken(userId, role, sessionId)

        saveRefreshToken(userId, sessionId, newRefreshToken)
        val maxAgeSeconds = (refreshTokenTtl / 1000).toInt()
        authCookieManager.addRefreshTokenCookie(response, newRefreshToken, maxAgeSeconds)

        return AccessTokenResponse(newAccessToken)
    }

    fun logout(
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        if (jwtProvider.isTokenValid(refreshToken, TokenType.REFRESH)) {
            deleteRefreshToken(
                jwtProvider.getUserIdFromToken(refreshToken),
                jwtProvider.getSessionIdFromToken(refreshToken),
            )
        }

        authCookieManager.expireRefreshTokenCookie(response)
    }

    fun issueWsToken(userId: UUID): WsTokenResponse {
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "ws-token:$token",
            userId.toString(),
            Duration.ofMinutes(1),
        )
        return WsTokenResponse(token)
    }

    fun deleteAllRefreshTokens(userId: UUID) {
        val sessionIds = redisTemplate.opsForSet().members(refreshSessionsKey(userId)).orEmpty()
        redisTemplate.delete(
            sessionIds.map { refreshTokenKey(userId, UUID.fromString(it)) } + refreshSessionsKey(userId),
        )
    }

    fun validateAndConsumeWsToken(token: String): UUID {
        val key = "ws-token:$token"
        val userId =
            redisTemplate.opsForValue().getAndDelete(key)
                ?: throw BusinessException(
                    ErrorCode.INVALID_TOKEN,
                    "Invalid or expired WebSocket token",
                )
        return UUID.fromString(userId)
    }

    // 세션 목록은 탈퇴 시 지울 토큰 키를 찾는 용도라 실제 세션을 빠짐없이 담아야 함
    // 목록에 먼저 넣고 토큰을 저장해 중간에 실패해도 목록에서 빠지는 세션이 없게 함
    // 목록의 만료도 갱신마다 늘려야 갱신하며 오래 쓰는 세션이 목록보다 오래 살지 않음
    private fun saveRefreshToken(
        userId: UUID,
        sessionId: UUID,
        refreshToken: String,
    ) {
        val ttl = Duration.ofMillis(refreshTokenTtl)
        redisTemplate.opsForSet().add(refreshSessionsKey(userId), sessionId.toString())
        redisTemplate.expire(refreshSessionsKey(userId), ttl)
        redisTemplate.opsForValue().set(refreshTokenKey(userId, sessionId), refreshToken, ttl)
    }

    // 토큰을 먼저 지워, 목록에서는 빠졌는데 토큰은 살아 있는 세션이 생기지 않게 함
    private fun deleteRefreshToken(
        userId: UUID,
        sessionId: UUID,
    ) {
        redisTemplate.delete(refreshTokenKey(userId, sessionId))
        redisTemplate.opsForSet().remove(refreshSessionsKey(userId), sessionId.toString())
    }

    companion object {
        private fun refreshTokenKey(
            userId: UUID,
            sessionId: UUID,
        ) = "refresh-token:$userId:$sessionId"

        private fun refreshSessionsKey(userId: UUID) = "refresh-sessions:$userId"
    }
}
