package io.waggle.waggleapiserver.domain.team.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "팀 생성/수정 요청 DTO")
data class TeamUpsertRequest(
    @Schema(description = "팀명", example = "Waggle")
    @field:NotBlank
    val name: String,
    @Schema(description = "팀 상세 설명")
    @field:NotBlank
    val description: String,
    @Schema(description = "팀 프로필 이미지 URL")
    val profileImageUrl: String?,
)
