package io.waggle.waggleapiserver.domain.post.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.validation.constraint.UniquePosition
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "모집글 수정 요청 DTO")
data class PostUpdateRequest(
    @Schema(description = "모집글 제목")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @Schema(description = "모집글 내용")
    @field:NotBlank
    @field:Size(max = 10000)
    val content: String,
    @Schema(description = "모집 정보")
    @field:Valid
    @field:NotNull
    @field:UniquePosition
    val recruitments: List<RecruitmentUpsertRequest>,
    @JsonProperty(required = true)
    @Schema(
        description = "모집 마감일. 그날 24시(KST)까지 모집. 무기한이면 null",
        example = "2026-09-30",
        types = ["string", "null"],
    )
    val deadline: LocalDate?,
)
