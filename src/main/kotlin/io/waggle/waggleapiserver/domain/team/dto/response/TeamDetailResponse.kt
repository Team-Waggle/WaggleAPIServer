package io.waggle.waggleapiserver.domain.team.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.team.Team
import java.time.Instant

@Schema(description = "팀 상세 응답 DTO")
data class TeamDetailResponse(
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long,
    @Schema(description = "팀명", example = "Waggle")
    val name: String,
    @Schema(description = "팀 설명")
    val description: String,
    @Schema(description = "팀 멤버 목록")
    val members: List<MemberResponse>,
    @Schema(description = "팀 생성 일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "팀 수정 일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun of(
            team: Team,
            members: List<MemberResponse>,
        ) = TeamDetailResponse(
            teamId = team.id,
            name = team.name,
            description = team.description,
            members = members,
            createdAt = team.createdAt,
            updatedAt = team.updatedAt,
        )
    }
}
