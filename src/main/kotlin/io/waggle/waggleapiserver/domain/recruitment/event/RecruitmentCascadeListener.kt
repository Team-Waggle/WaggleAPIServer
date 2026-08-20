package io.waggle.waggleapiserver.domain.recruitment.event

import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class RecruitmentCascadeListener(
    private val recruitmentRepository: RecruitmentRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        recruitmentRepository.deleteByPostTeamId(event.teamId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        recruitmentRepository.deleteByPostId(event.postId)
    }
}
