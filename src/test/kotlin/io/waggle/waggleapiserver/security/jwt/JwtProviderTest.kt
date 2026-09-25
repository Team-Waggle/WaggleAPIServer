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

        assertThat(jwtProvider.isTokenValid(token)).isTrue()
    }

    @Test
    fun `다른 키로 서명한 토큰은 예외 없이 무효 처리함`() {
        val forgedToken =
            JwtProvider(OTHER_SECRET, TTL, TTL).generateRefreshToken(UUID.randomUUID(), UserRole.USER)

        assertThat(jwtProvider.isTokenValid(forgedToken)).isFalse()
    }

    @Test
    fun `만료된 토큰은 무효 처리함`() {
        val expiredToken =
            JwtProvider(SECRET, -1_000L, -1_000L).generateAccessToken(UUID.randomUUID(), UserRole.USER)

        assertThat(jwtProvider.isTokenValid(expiredToken)).isFalse()
    }

    @Test
    fun `형식이 깨진 토큰은 무효 처리함`() {
        assertThat(jwtProvider.isTokenValid("not.a.jwt")).isFalse()
    }

    @Test
    fun `빈 토큰은 무효 처리함`() {
        assertThat(jwtProvider.isTokenValid("")).isFalse()
    }
}
