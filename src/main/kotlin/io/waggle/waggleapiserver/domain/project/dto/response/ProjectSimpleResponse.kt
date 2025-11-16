package io.waggle.waggleapiserver.domain.project.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.project.Project
import java.time.Instant

@Schema(description = "프로젝트 응답 DTO")
data class ProjectSimpleResponse(
    @Schema(description = "프로젝트 ID", example = "1")
    val projectId: Long,
    @Schema(description = "프로젝트명", example = "Waggle")
    val name: String,
    @Schema(description = "프로젝트 설명")
    val description: String,
    @Schema(description = "프로젝트 생성 일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "프로젝트 수정 일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun from(project: Project) =
            ProjectSimpleResponse(
                projectId = project.id,
                name = project.name,
                description = project.description,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
            )
    }
}
