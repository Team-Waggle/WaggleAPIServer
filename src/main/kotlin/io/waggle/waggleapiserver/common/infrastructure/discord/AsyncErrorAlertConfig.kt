package io.waggle.waggleapiserver.common.infrastructure.discord

import io.waggle.waggleapiserver.common.util.logger
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class AsyncErrorAlertConfig(
    private val discordWebhookClientProvider: ObjectProvider<DiscordWebhookClient>,
    private val taskExecutorBuilder: ThreadPoolTaskExecutorBuilder,
    private val taskSchedulerBuilder: ThreadPoolTaskSchedulerBuilder,
) : AsyncConfigurer,
    SchedulingConfigurer {
    // WebSocket 설정이 실행기 빈을 여럿 등록해 Boot의 기본 실행기가 생기지 않음
    // 지정하지 않으면 @Async가 작업마다 스레드를 새로 만드는 SimpleAsyncTaskExecutor로 떨어짐
    // 풀 크기와 종료 대기는 spring.task.execution 설정을 따름
    @Bean
    fun applicationTaskExecutor(): ThreadPoolTaskExecutor =
        taskExecutorBuilder.build().apply {
            // AbortPolicy면 AFTER_COMMIT 리스너 호출부에 예외가 터져 이미 커밋된 요청이 500이 됨
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        }

    // 빈이어야 컨텍스트 종료 시 spring.task.scheduling.shutdown 설정대로 진행 중인 작업을 기다림
    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler =
        taskSchedulerBuilder
            .poolSize(SCHEDULER_POOL_SIZE)
            .threadNamePrefix("scheduled-")
            .build()
            .apply {
                setErrorHandler { throwable ->
                    logger.error("Uncaught scheduled task exception", throwable)
                    notifyDiscord("@Scheduled task", throwable)
                }
            }

    override fun getAsyncExecutor(): Executor = applicationTaskExecutor()

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        AsyncUncaughtExceptionHandler { throwable, method, _ ->
            val source = "@Async ${method.declaringClass.simpleName}.${method.name}"
            logger.error("Uncaught async exception in $source", throwable)
            notifyDiscord(source, throwable)
        }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler())
    }

    private fun notifyDiscord(
        source: String,
        throwable: Throwable,
    ) {
        try {
            discordWebhookClientProvider
                .getIfAvailable()
                ?.send(DiscordErrorContext.fromAsync(source, throwable))
        } catch (notifyError: Exception) {
            logger.warn("Failed to enqueue Discord notification for async exception", notifyError)
        }
    }

    companion object {
        // ScheduledThreadPoolExecutor는 고정 크기라 런타임 동적 조절 불가
        // 부팅 시점에 CPU 코어 수로 맞추되 단일 코어 환경에서도 최소 2 보장
        private val SCHEDULER_POOL_SIZE = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    }
}
