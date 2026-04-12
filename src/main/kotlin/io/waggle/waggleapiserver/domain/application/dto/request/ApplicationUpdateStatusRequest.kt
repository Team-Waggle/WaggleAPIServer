package io.waggle.waggleapiserver.domain.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import jakarta.validation.constraints.NotNull

@Schema(description = "지원 상태 변경 요청 DTO")
data class ApplicationUpdateStatusRequest(
    @Schema(description = "변경할 상태", example = "APPROVED")
    @field:NotNull
    val status: ApplicationStatus,
)
