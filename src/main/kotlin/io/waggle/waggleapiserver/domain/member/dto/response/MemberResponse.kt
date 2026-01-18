package io.waggle.waggleapiserver.domain.member.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant
import java.util.UUID

@Schema(description = "멤버 응답 DTO")
data class MemberResponse(
    @Schema(description = "멤버 ID", example = "1")
    val memberId: Long,
    @Schema(description = "프로젝트 ID", example = "1")
    val projectId: Long,
    @Schema(description = "멤버 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,
    @Schema(description = "멤버 역할", example = "LEADER")
    val role: MemberRole,
    @Schema(description = "사용자명", example = "testUser")
    val username: String,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://avatars.githubusercontent.com/u/112466204?s=80&v=4",
    )
    val profileImageUrl: String?,
    @Schema(description = "멤버 합류 일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    companion object {
        fun of(
            member: Member,
            user: User,
        ): MemberResponse =
            MemberResponse(
                memberId = member.id,
                projectId = member.projectId,
                userId = member.userId,
                role = member.role,
                username = user.username!!,
                profileImageUrl = user.profileImageUrl,
                createdAt = member.createdAt,
            )
    }
}
