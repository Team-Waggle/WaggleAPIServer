package io.waggle.waggleapiserver.domain.application.event

import io.waggle.waggleapiserver.domain.application.repository.ApplicationReadRepository
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class ApplicationCascadeListener(
    private val applicationRepository: ApplicationRepository,
    private val applicationReadRepository: ApplicationReadRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        applicationReadRepository.updateDeletedAtByApplicationTeamIdAndDeletedAtIsNull(event.teamId)
        applicationRepository.updateDeletedAtByTeamIdAndDeletedAtIsNull(event.teamId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        applicationReadRepository.updateDeletedAtByApplicationPostIdAndDeletedAtIsNull(event.postId)
        applicationRepository.updateDeletedAtByPostIdAndDeletedAtIsNull(event.postId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        applicationReadRepository.updateDeletedAtByUserIdAndDeletedAtIsNull(event.userId)
        applicationReadRepository.updateDeletedAtByApplicationUserIdAndDeletedAtIsNull(event.userId)
        applicationReadRepository.updateDeletedAtByApplicationPostUserIdAndDeletedAtIsNull(event.userId)
        applicationRepository.updateDeletedAtByUserIdAndDeletedAtIsNull(event.userId)
        applicationRepository.updateDeletedAtByPostUserIdAndDeletedAtIsNull(event.userId)
    }
}
