package io.waggle.waggleapiserver.domain.message.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class MessageSendRequest(
    @field:NotNull val receiverId: UUID,
    @field:NotBlank val content: String,
)
