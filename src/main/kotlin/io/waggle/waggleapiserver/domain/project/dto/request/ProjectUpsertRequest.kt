package io.waggle.waggleapiserver.domain.project.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "프로젝트 생성/수정 요청 DTO")
data class ProjectUpsertRequest(
    @Schema(description = "프로젝트명", example = "Waggle")
    @field:NotBlank
    val name: String,
    @Schema(description = "프로젝트 상세 설명")
    @field:NotBlank
    val description: String,
    @Schema(description = "프로젝트 섬네일 URL")
    val thumbnailUrl: String?,
)
