package io.waggle.waggleapiserver.domain.follow.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "팔로우 토글 요청 DTO")
data class FollowToggleRequest(
    @Schema(description = "팔로우 대상 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @field:NotNull
    val userId: UUID,
)
