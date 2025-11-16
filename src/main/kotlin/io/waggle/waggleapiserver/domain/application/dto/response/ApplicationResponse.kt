package io.waggle.waggleapiserver.domain.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.time.Instant
import java.util.UUID

@Schema(description = "지원 응답 DTO")
data class ApplicationResponse(
    @Schema(description = "지원 ID", example = "1")
    val applicationId: Long,
    @Schema(description = "지원 직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "지원 상태", example = "APPROVED")
    val status: ApplicationStatus,
    @Schema(description = "지원 프로젝트 ID", example = "1")
    val projectId: Long,
    @Schema(description = "지원자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,
    @Schema(description = "지원 일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(application: Application): ApplicationResponse =
            ApplicationResponse(
                applicationId = application.id,
                position = application.position,
                status = application.status,
                projectId = application.projectId,
                userId = application.userId,
                createdAt = application.createdAt,
            )
    }
}
