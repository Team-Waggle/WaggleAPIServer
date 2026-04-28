package io.waggle.waggleapiserver.domain.notification.event

import io.waggle.waggleapiserver.domain.user.enums.Position
import java.util.UUID

data class ApplicationReceivedEvent(
    val teamId: Long,
    val postId: Long,
    val position: Position,
    val triggeredBy: UUID,
)

data class ApplicationRejectedEvent(
    val teamId: Long,
    val applicantUserId: UUID,
    val triggeredBy: UUID,
)

data class TeamJoinedEvent(
    val teamId: Long,
    val joinedUserId: UUID,
    val triggeredBy: UUID,
)

data class MemberJoinedEvent(
    val teamId: Long,
    val triggeredBy: UUID,
)

data class MemberLeftEvent(
    val teamId: Long,
    val triggeredBy: UUID,
)

data class MemberRemovedEvent(
    val teamId: Long,
    val removedUserId: UUID,
    val triggeredBy: UUID,
)

data class TeamCompletedEvent(
    val teamId: Long,
)

data class ReviewReceivedEvent(
    val teamId: Long,
    val revieweeId: UUID,
)
