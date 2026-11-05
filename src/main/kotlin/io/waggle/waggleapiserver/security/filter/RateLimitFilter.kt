package io.waggle.waggleapiserver.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    // TODO: 다중 서버 환경 시 bucket4j-redis로 전환 필요
    private val buckets = ConcurrentHashMap<String, Bucket>()

    @Scheduled(fixedRate = 600_000)
    fun cleanUpExpiredBuckets() {
        buckets.entries.removeIf { it.value.availableTokens == BUCKET_CAPACITY }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // prod는 forward-headers-strategy: native라 Tomcat이 신뢰 프록시(nginx)의 X-Forwarded-For로 remoteAddr을 바꿔 둠
        // 헤더를 직접 읽으면 클라이언트가 값을 위조해 버킷을 우회할 수 있음
        // native 설정이 빠지면 모든 요청이 nginx IP 하나로 모여 전체가 한 버킷을 나눠 씀
        val bucket = buckets.computeIfAbsent(request.remoteAddr) { createBucket() }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(
                objectMapper.writeValueAsString(
                    ErrorResponse(
                        status = HttpStatus.TOO_MANY_REQUESTS.value(),
                        code = "TOO_MANY_REQUESTS",
                        message = "Too many requests. Please try again later.",
                    ),
                ),
            )
        }
    }

    private fun createBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(
                Bandwidth
                    .builder()
                    .capacity(BUCKET_CAPACITY)
                    .refillGreedy(BUCKET_REFILL_PER_MINUTE, Duration.ofMinutes(1))
                    .build(),
            ).build()

    companion object {
        private const val BUCKET_CAPACITY = 1000L
        private const val BUCKET_REFILL_PER_MINUTE = 1000L
    }
}
