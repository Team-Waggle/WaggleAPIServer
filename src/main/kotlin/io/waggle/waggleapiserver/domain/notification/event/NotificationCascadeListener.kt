package io.waggle.waggleapiserver.domain.notification.event

import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationCascadeListener(
    private val notificationRepository: NotificationRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        notificationRepository.deleteByPostInTeamId(event.teamId)
        notificationRepository.deleteByTeamId(event.teamId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        notificationRepository.deleteByPostId(event.postId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        notificationRepository.deleteByPostUserId(event.userId)
        notificationRepository.deleteByUserId(event.userId)
    }
}
