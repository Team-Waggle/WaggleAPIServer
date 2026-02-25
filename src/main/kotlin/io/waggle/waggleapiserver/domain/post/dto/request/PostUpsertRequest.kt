package io.waggle.waggleapiserver.domain.post.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "모집글 생성/수정 요청 DTO")
data class PostUpsertRequest(
    @Schema(description = "팀 ID", example = "1")
    @field:NotNull
    val teamId: Long,
    @Schema(description = "모집글 제목")
    @field:NotBlank
    val title: String,
    @Schema(description = "모집글 내용")
    @field:NotBlank
    val content: String,
    @Schema(description = "모집 정보")
    @field:NotNull
    val recruitments: List<RecruitmentUpsertRequest>,
    @Schema(description = "필요 스킬 목록", example = "[\"JAVA\", \"SPRING\"]")
    val skills: Set<Skill> = emptySet(),
)
