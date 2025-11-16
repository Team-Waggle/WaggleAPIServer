package io.waggle.waggleapiserver.domain.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.notification.Notification
import java.time.Instant

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    @Schema(description = "알림 ID", example = "1")
    val notificationId: Long,
    @Schema(description = "알림 제목", example = "지원 승인")
    val title: String,
    @Schema(description = "알림 내용", example = "와글 프로젝트 지원이 승인되었습니다. 팀에 참여해 주세요!")
    val content: String,
    @Schema(description = "프로젝트 리다이렉트 URL", example = "https://waggle.store")
    val redirectUrl: String,
    @Schema(description = "알림 읽음 여부", example = "false")
    val isRead: Boolean,
    @Schema(description = "알림 생성 일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                notification.id,
                notification.title,
                notification.content,
                notification.redirectUrl,
                notification.isRead,
                notification.createdAt,
            )
    }
}
