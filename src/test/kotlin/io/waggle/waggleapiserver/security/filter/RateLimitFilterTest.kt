package io.waggle.waggleapiserver.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RateLimitFilterTest {
    companion object {
        // 버킷이 분당 1000개를 greedy refill하므로 한도만큼만 보내면 도중에 채워진 토큰으로 통과할 수 있음
        // 두 배를 보내면 1분 안에 끝나는 한 반드시 429가 섞임
        private const val BURST_SIZE = 2000
    }

    private val rateLimitFilter = RateLimitFilter(ObjectMapper())

    private fun requestStatus(
        remoteAddr: String,
        forwardedFor: String? = null,
    ): Int {
        val request =
            MockHttpServletRequest("GET", "/posts").apply {
                this.remoteAddr = remoteAddr
                forwardedFor?.let { addHeader("X-Forwarded-For", it) }
            }
        val response = MockHttpServletResponse()
        rateLimitFilter.doFilter(request, response, MockFilterChain())
        return response.status
    }

    @Test
    fun `X-Forwarded-For를 바꿔도 같은 remoteAddr이면 한 버킷을 씀`() {
        val statuses =
            (0 until BURST_SIZE).map { index ->
                requestStatus(remoteAddr = "203.0.113.1", forwardedFor = "198.51.${index / 256}.${index % 256}")
            }

        assertThat(statuses).contains(429)
    }

    @Test
    fun `remoteAddr이 다르면 버킷을 따로 씀`() {
        val statuses = (0 until BURST_SIZE).map { requestStatus(remoteAddr = "203.0.113.2") }

        assertThat(statuses).contains(429)
        assertThat(requestStatus(remoteAddr = "203.0.113.3")).isEqualTo(200)
    }
}
