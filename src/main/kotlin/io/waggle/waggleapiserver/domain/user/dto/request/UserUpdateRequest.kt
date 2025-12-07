package io.waggle.waggleapiserver.domain.user.dto.request

import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UserUpdateRequest(
    @field:NotBlank val username: String,
    @field:NotNull val position: Position,
    val bio: String?,
)
