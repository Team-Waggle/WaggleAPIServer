package io.waggle.waggleapiserver.domain.team.event

import java.util.UUID

data class TeamDeletedEvent(
    val teamId: Long,
    val deletedBy: UUID,
)
