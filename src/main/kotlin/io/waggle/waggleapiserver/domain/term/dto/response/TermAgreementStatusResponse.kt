package io.waggle.waggleapiserver.domain.term.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "약관 동의 여부 응답 DTO")
data class TermAgreementStatusResponse(
    @Schema(description = "현재 사용자가 필수 약관 모두 동의했는지 여부", example = "true")
    val agreed: Boolean,
)
