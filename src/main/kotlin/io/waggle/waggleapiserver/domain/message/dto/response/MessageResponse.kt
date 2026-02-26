package io.waggle.waggleapiserver.domain.message.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.message.Message
import java.time.Instant
import java.util.UUID

@Schema(description = "메시지 응답 DTO")
data class MessageResponse(
    @Schema(description = "메시지 ID", example = "1")
    val messageId: Long,
    @Schema(description = "발신자 ID", example = "018c8f4e-3b2a-7000-8000-123456789abc")
    val senderId: UUID,
    @Schema(description = "수신자 ID", example = "018c8f4e-3b2c-7000-8000-345678901234")
    val receiverId: UUID,
    @Schema(description = "메시지 내용", example = "Hello, World!")
    val content: String,
    @Schema(description = "발송일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "확인일시", example = "2025-11-30T12:30:45.123456Z")
    val readAt: Instant?,
) {
    companion object {
        fun from(message: Message): MessageResponse =
            MessageResponse(
                messageId = message.id,
                senderId = message.senderId,
                receiverId = message.receiverId,
                content = message.content,
                createdAt = message.createdAt,
                readAt = message.readAt,
            )
    }
}
