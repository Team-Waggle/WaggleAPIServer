package io.waggle.waggleapiserver.common.infrastructure.discord

import io.waggle.waggleapiserver.support.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Import(AsyncErrorAlertConfigTest.AsyncProbeConfig::class)
class AsyncErrorAlertConfigTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var asyncProbe: AsyncProbe

    @Autowired
    private lateinit var applicationTaskExecutor: ThreadPoolTaskExecutor

    @Test
    fun `Async 메서드는 상한이 있는 공용 풀에서 실행된다`() {
        val threadName = asyncProbe.currentThreadName().get(5, TimeUnit.SECONDS)

        assertThat(threadName).startsWith("async-")
        assertThat(applicationTaskExecutor.corePoolSize).isEqualTo(4)
        assertThat(applicationTaskExecutor.maxPoolSize).isEqualTo(8)
        assertThat(applicationTaskExecutor.queueCapacity).isEqualTo(500)
        assertThat(applicationTaskExecutor.threadPoolExecutor.rejectedExecutionHandler)
            .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy::class.java)
        assertWaitsOnShutdown(applicationTaskExecutor)
    }

    private fun assertWaitsOnShutdown(executor: Any) {
        assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown")).isEqualTo(true)
        assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis")).isEqualTo(10_000L)
    }

    @TestConfiguration
    class AsyncProbeConfig {
        @Bean
        fun asyncProbe() = AsyncProbe()
    }

    open class AsyncProbe {
        @Async
        open fun currentThreadName(): CompletableFuture<String> =
            CompletableFuture.completedFuture(Thread.currentThread().name)
    }
}
