package io.waggle.waggleapiserver.common.infrastructure.discord

import jakarta.servlet.http.HttpServletRequest
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DiscordErrorContext(
    val exceptionClass: String,
    val message: String,
    val requestUri: String,
    val httpMethod: String,
    val queryString: String?,
    val userAgent: String?,
    val referer: String?,
    val forwardedFor: String?,
    val host: String?,
    val stackTrace: String,
    val timestamp: String,
) {
    companion object {
        private const val STACK_TRACE_LINE_LIMIT = 15
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(request: HttpServletRequest, exception: Exception): DiscordErrorContext {
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            val stackLines = sw.toString().lineSequence().take(STACK_TRACE_LINE_LIMIT).joinToString("\n")

            return DiscordErrorContext(
                exceptionClass = exception.javaClass.name,
                message = exception.message ?: "No message",
                requestUri = request.requestURI,
                httpMethod = request.method,
                queryString = request.queryString,
                userAgent = request.getHeader("User-Agent"),
                referer = request.getHeader("Referer"),
                forwardedFor = request.getHeader("X-Forwarded-For"),
                host = request.getHeader("Host"),
                stackTrace = stackLines,
                timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT),
            )
        }
    }
}
