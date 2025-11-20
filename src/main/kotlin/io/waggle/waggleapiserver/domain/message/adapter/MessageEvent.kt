package io.waggle.waggleapiserver.domain.message.adapter

import java.util.UUID

data class MessageEvent(
    val messageId: Long,
    val receiverId: UUID,
)
