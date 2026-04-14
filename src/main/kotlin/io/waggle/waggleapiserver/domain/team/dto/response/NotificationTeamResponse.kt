package io.waggle.waggleapiserver.domain.team.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.team.Team

@Schema(description = "알림 팀 응답 DTO")
data class NotificationTeamResponse(
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long,
    @Schema(description = "팀명", example = "Waggle")
    val name: String,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://example.com.png",
    )
    val profileImageUrl: String?,
) {
    companion object {
        fun from(team: Team) =
            NotificationTeamResponse(
                teamId = team.id,
                name = team.name,
                profileImageUrl = team.profileImageUrl,
            )
    }
}
