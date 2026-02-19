package io.waggle.waggleapiserver.domain.notification.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.notification.NotificationType
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "알림 생성 요청 DTO")
data class NotificationCreateRequest(
    @Schema(description = "알림 타입", example = "APPLICATION_RECEIVED")
    @field:NotNull
    val type: NotificationType,
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long? = null,
    @Schema(description = "알림 대상 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @field:NotNull
    val userId: UUID,
)
