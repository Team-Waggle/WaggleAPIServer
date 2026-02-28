package io.waggle.waggleapiserver.common.infrastructure.discord

import io.waggle.waggleapiserver.common.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Profile("prod")
@Component
class DiscordWebhookClient(
    @Value("\${app.discord.webhook-url:}")
    private val webhookUrl: String,
) {
    private val restTemplate = RestTemplate()

    @Async
    fun send(context: DiscordErrorContext) {
        if (webhookUrl.isBlank()) {
            return
        }

        try {
            val message = buildMessage(context)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val body = mapOf("content" to message.take(2000))
            val request = HttpEntity(body, headers)

            restTemplate.postForEntity(webhookUrl, request, String::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to send Discord webhook notification", e)
        }
    }

    private fun buildMessage(context: DiscordErrorContext): String {
        return buildString {
            appendLine("## 🚨 서버 에러 발생")
            appendLine("**시각**: ${context.timestamp}")
            appendLine("**예외**: `${context.exceptionClass}`")
            appendLine("**메시지**: ${context.message}")
            appendLine()
            appendLine("**요청**: `${context.httpMethod} ${context.requestUri}`")
            context.queryString?.let { appendLine("**쿼리**: `$it`") }
            appendLine()
            appendLine("**Headers**")
            context.host?.let { appendLine("- Host: `$it`") }
            context.userAgent?.let { appendLine("- User-Agent: `$it`") }
            context.referer?.let { appendLine("- Referer: `$it`") }
            context.forwardedFor?.let { appendLine("- X-Forwarded-For: `$it`") }
            appendLine()
            appendLine("**Stack Trace**")
            appendLine("```")
            appendLine(context.stackTrace)
            appendLine("```")
        }
    }
}
