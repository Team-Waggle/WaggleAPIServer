package io.waggle.waggleapiserver.domain.user.event

import java.util.UUID

data class UserDeactivatedEvent(
    val userId: UUID,
)
