package io.waggle.waggleapiserver.domain.member.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.member.MemberRole
import jakarta.validation.constraints.NotNull

@Schema(description = "멤버 수정 요청 DTO")
data class MemberUpdateRoleRequest(
    @Schema(description = "멤버 역할", example = "MANAGER")
    @field:NotNull val role: MemberRole,
)
