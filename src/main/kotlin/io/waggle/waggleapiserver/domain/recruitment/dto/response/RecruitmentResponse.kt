package io.waggle.waggleapiserver.domain.recruitment.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position

@Schema(description = "모집 정보 응답 DTO")
class RecruitmentResponse(
    @Schema(description = "모집 ID", example = "1")
    val recruitmentId: Long,
    @Schema(description = "모집 직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "모집 중인 인원 수", example = "3")
    val recruitingCount: Int,
) {
    companion object {
        fun from(recruitment: Recruitment): RecruitmentResponse =
            RecruitmentResponse(
                recruitmentId = recruitment.id,
                position = recruitment.position,
                recruitingCount = recruitment.recruitingCount,
            )
    }
}
