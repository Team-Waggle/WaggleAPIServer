package io.waggle.waggleapiserver.domain.notification.dto.request

import io.waggle.waggleapiserver.domain.notification.NotificationType
import java.util.UUID

data class NotificationCreateRequest(
    val type: NotificationType,
    val projectId: Long?,
    val userId: UUID,
)
