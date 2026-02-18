package io.waggle.waggleapiserver.domain.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

@Schema(description = "사용자 프로필 초기 설정 요청 DTO")
data class UserSetupProfileRequest(
    @Schema(description = "사용자명", example = "testUser")
    @field:NotBlank
    val username: String,
    @Schema(description = "직무", example = "BACKEND")
    @field:NotNull
    val position: Position,
    @Schema(description = "본인 소개")
    @field:Size(max = 1000)
    val bio: String?,
    @Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
    @Schema(description = "기술 스택", example = "[\"KOTLIN\", \"SPRING\"]")
    @field:NotNull
    val skills: Set<Skill>,
    @Schema(
        description = "포트폴리오 URL 목록",
        example = "[\"https://github.com/user\", \"https://blog.example.com\"]",
    )
    val portfolioUrls: List<@URL String> = emptyList(),
)
