package io.waggle.waggleapiserver.domain.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.util.UUID

@Schema(description = "사용자 상세 응답 DTO")
data class UserDetailResponse(
    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,
    @Schema(description = "사용자명", example = "sillysillyman")
    val username: String,
    @Schema(description = "이메일", example = "sillysillyman.cs@gmail.com")
    val email: String,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://avatars.githubusercontent.com/u/112466204?s=80&v=4",
    )
    val profileImageUrl: String?,
    @Schema(description = "직무", example = "BACKEND")
    val position: Position?,
    @Schema(description = "자기소개")
    val bio: String?,
) {
    companion object {
        fun from(user: User): UserDetailResponse =
            UserDetailResponse(
                user.id,
                user.username!!,
                user.email,
                user.profileImageUrl,
                user.position,
                user.bio,
            )
    }
}
