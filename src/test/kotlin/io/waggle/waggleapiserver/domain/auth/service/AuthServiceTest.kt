package io.waggle.waggleapiserver.domain.auth.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.auth.AuthCookieManager
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.jwt.JwtProvider
import io.waggle.waggleapiserver.support.TestContainers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class AuthServiceTest {
    private val redisTemplate =
        StringRedisTemplate(
            LettuceConnectionFactory(
                RedisStandaloneConfiguration(TestContainers.redis.host, TestContainers.redis.firstMappedPort),
            ).apply { afterPropertiesSet() },
        ).apply { afterPropertiesSet() }

    private val jwtProvider = JwtProvider(SECRET, ACCESS_TOKEN_TTL, REFRESH_TOKEN_TTL)

    private val authService =
        AuthService(
            REFRESH_TOKEN_TTL,
            AuthCookieManager(cookieSecure = false, cookieSameSite = "Lax", cookieDomain = null),
            jwtProvider,
            redisTemplate,
        )

    @Test
    fun `다른 기기에서 로그인해도 기존 기기의 세션이 유지됨`() {
        val userId = UUID.randomUUID()
        val laptop = login(userId)
        val phone = login(userId)

        val laptopRotated = refresh(laptop)
        val phoneRotated = refresh(phone)

        assertThat(refresh(laptopRotated)).isNotBlank()
        assertThat(refresh(phoneRotated)).isNotBlank()
    }

    @Test
    fun `이미 교체된 리프레시 토큰을 다시 쓰면 그 세션만 끊김`() {
        val userId = UUID.randomUUID()
        val stolen = login(userId)
        val phone = login(userId)
        val laptopRotated = refresh(stolen)

        assertInvalidToken { refresh(stolen) }

        assertInvalidToken { refresh(laptopRotated) }
        assertThat(refresh(phone)).isNotBlank()
    }

    @Test
    fun `로그아웃은 그 기기의 세션만 끝냄`() {
        val userId = UUID.randomUUID()
        val laptop = login(userId)
        val phone = login(userId)

        authService.logout(laptop, MockHttpServletResponse())

        assertInvalidToken { refresh(laptop) }
        assertThat(refresh(phone)).isNotBlank()
        assertThat(sessionIdsOf(userId)).containsExactly(jwtProvider.getSessionIdFromToken(phone).toString())
    }

    @Test
    fun `탈퇴하면 그 사용자의 모든 세션이 끝나고 다른 사용자는 영향이 없음`() {
        val userId = UUID.randomUUID()
        val laptop = login(userId)
        val phone = login(userId)
        val otherUser = login(UUID.randomUUID())

        authService.deleteAllRefreshTokens(userId)

        assertInvalidToken { refresh(laptop) }
        assertInvalidToken { refresh(phone) }
        assertThat(refresh(otherUser)).isNotBlank()
        assertThat(redisTemplate.hasKey("refresh-sessions:$userId")).isFalse()
    }

    @Test
    fun `갱신하면 세션 목록의 만료도 함께 늘어남`() {
        val userId = UUID.randomUUID()
        val laptop = login(userId)
        redisTemplate.expire("refresh-sessions:$userId", Duration.ofSeconds(10))

        refresh(laptop)

        assertThat(redisTemplate.getExpire("refresh-sessions:$userId", TimeUnit.MILLISECONDS))
            .isGreaterThan(REFRESH_TOKEN_TTL - 60_000)
    }

    @Test
    fun `액세스 토큰으로는 갱신할 수 없음`() {
        val accessToken = jwtProvider.generateAccessToken(UUID.randomUUID(), UserRole.USER)

        assertInvalidToken { refresh(accessToken) }
    }

    @Test
    fun `세션 ID와 종류가 없는 이전 형식의 리프레시 토큰은 거부함`() {
        val userId = UUID.randomUUID()
        val legacyToken =
            Jwts
                .builder()
                .subject(userId.toString())
                .claim("role", UserRole.USER.name)
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL))
                .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray()))
                .compact()
        redisTemplate.opsForValue().set("refresh-token:$userId", legacyToken)

        assertInvalidToken { refresh(legacyToken) }
    }

    private fun sessionIdsOf(userId: UUID): Set<String> =
        redisTemplate.opsForSet().members("refresh-sessions:$userId").orEmpty()

    private fun login(userId: UUID): String {
        val ott = authService.issueOttForOAuth(userId, UserRole.USER)
        return redisTemplate.opsForValue().get("oauth-ott-refresh:$ott")!!
    }

    // 갱신 후 쿠키로 내려온 새 리프레시 토큰을 돌려줌
    private fun refresh(refreshToken: String): String {
        val response = MockHttpServletResponse()
        authService.refresh(refreshToken, response)
        return REFRESH_COOKIE.find(response.getHeader("Set-Cookie")!!)!!.groupValues[1]
    }

    private fun assertInvalidToken(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_TOKEN)
    }

    companion object {
        private const val SECRET = "test-secret-test-secret-test-secret-test-secret"
        private const val ACCESS_TOKEN_TTL = 3_600_000L
        private const val REFRESH_TOKEN_TTL = 604_800_000L
        private val REFRESH_COOKIE = Regex("refreshToken=([^;]+)")
    }
}
