package io.waggle.waggleapiserver.domain.member.dto.request

import io.waggle.waggleapiserver.domain.member.MemberRole
import jakarta.validation.constraints.NotNull

data class MemberUpdateRoleRequest(
    @field:NotNull val projectId: Long,
    @field:NotNull val role: MemberRole,
)
