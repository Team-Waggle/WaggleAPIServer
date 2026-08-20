package io.waggle.waggleapiserver.domain.follow.event

import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class FollowCascadeListener(
    private val followRepository: FollowRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        followRepository.updateDeletedAtByFollowerIdOrFolloweeIdAndDeletedAtIsNull(event.userId)
    }
}
