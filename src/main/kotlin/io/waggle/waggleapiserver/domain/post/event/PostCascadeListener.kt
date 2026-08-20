package io.waggle.waggleapiserver.domain.post.event

import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class PostCascadeListener(
    private val postRepository: PostRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        postRepository.updateDeletedAtByTeamIdAndDeletedAtIsNull(event.teamId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        postRepository.updateDeletedAtByUserIdAndDeletedAtIsNull(event.userId)
    }
}
