package io.waggle.waggleapiserver.security.jwt

import io.waggle.waggleapiserver.domain.user.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtProviderTest {
    companion object {
        private const val SECRET = "test-secret-test-secret-test-secret-test-secret"
        private const val OTHER_SECRET = "other-secret-other-secret-other-secret-other"
        private const val TTL = 3_600_000L
    }

    private val jwtProvider = JwtProvider(SECRET, TTL, TTL)

    @Test
    fun `정상 토큰은 유효함`() {
        val token = jwtProvider.generateAccessToken(UUID.randomUUID(), UserRole.USER)

        assertThat(jwtProvider.isTokenValid(token, TokenType.ACCESS)).isTrue()
    }

    @Test
    fun `다른 키로 서명한 토큰은 예외 없이 무효 처리함`() {
        val forgedToken =
            JwtProvider(
                OTHER_SECRET,
                TTL,
                TTL,
            ).generateRefreshToken(UUID.randomUUID(), UserRole.USER, UUID.randomUUID())

        assertThat(jwtProvider.isTokenValid(forgedToken, TokenType.REFRESH)).isFalse()
    }

    @Test
    fun `만료된 토큰은 무효 처리함`() {
        val expiredToken =
            JwtProvider(SECRET, -1_000L, -1_000L).generateAccessToken(UUID.randomUUID(), UserRole.USER)

        assertThat(jwtProvider.isTokenValid(expiredToken, TokenType.ACCESS)).isFalse()
    }

    @Test
    fun `형식이 깨진 토큰은 무효 처리함`() {
        assertThat(jwtProvider.isTokenValid("not.a.jwt", TokenType.ACCESS)).isFalse()
    }

    @Test
    fun `빈 토큰은 무효 처리함`() {
        assertThat(jwtProvider.isTokenValid("", TokenType.ACCESS)).isFalse()
    }

    @Test
    fun `리프레시 토큰은 액세스 토큰으로, 액세스 토큰은 리프레시 토큰으로 인정하지 않음`() {
        val accessToken = jwtProvider.generateAccessToken(UUID.randomUUID(), UserRole.USER)
        val refreshToken = jwtProvider.generateRefreshToken(UUID.randomUUID(), UserRole.USER, UUID.randomUUID())

        assertThat(jwtProvider.isTokenValid(refreshToken, TokenType.ACCESS)).isFalse()
        assertThat(jwtProvider.isTokenValid(accessToken, TokenType.REFRESH)).isFalse()
        assertThat(jwtProvider.isTokenValid(refreshToken, TokenType.REFRESH)).isTrue()
    }

    @Test
    fun `리프레시 토큰은 세션 ID를 담고 같은 세션이라도 매번 다른 값으로 발급됨`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()

        val first = jwtProvider.generateRefreshToken(userId, UserRole.USER, sessionId)
        val second = jwtProvider.generateRefreshToken(userId, UserRole.USER, sessionId)

        assertThat(jwtProvider.getSessionIdFromToken(first)).isEqualTo(sessionId)
        assertThat(first).isNotEqualTo(second)
    }
}
