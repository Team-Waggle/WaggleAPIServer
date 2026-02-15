package io.waggle.waggleapiserver.domain.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "팀 지원 요청 DTO")
data class ApplicationCreateRequest(
    @Schema(description = "팀 지원 동기")
    @field:NotBlank
    @field:Size(max = 1000)
    val detail: String,
)
