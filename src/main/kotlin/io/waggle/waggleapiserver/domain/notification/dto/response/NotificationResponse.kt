package io.waggle.waggleapiserver.domain.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import java.time.Instant

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    @Schema(description = "알림 ID", example = "1")
    val notificationId: Long,
    @Schema(description = "알림 타입", example = "APPLICATION_RECEIVED")
    val type: NotificationType,
    @Schema(description = "프로젝트 정보")
    val project: ProjectSimpleResponse?,
    @Schema(description = "알림 확인 일시", example = "false")
    val readAt: Instant?,
    @Schema(description = "알림 생성 일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    companion object {
        fun of(
            notification: Notification,
            project: ProjectSimpleResponse?,
        ): NotificationResponse =
            NotificationResponse(
                notificationId = notification.id,
                type = notification.type,
                project = project,
                readAt = notification.readAt,
                createdAt = notification.createdAt,
            )
    }
}
