package io.waggle.waggleapiserver.domain.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "사용자 수정 요청 DTO")
data class UserUpdateRequest(
    @Schema(description = "사용자명", example = "sillysillyman")
    @field:NotBlank
    val username: String,
    @Schema(description = "직무", example = "BACKEND")
    @field:NotNull
    val position: Position,
    @Schema(description = "본인 소개")
    @field:Size(max = 1000)
    val bio: String?,
)
