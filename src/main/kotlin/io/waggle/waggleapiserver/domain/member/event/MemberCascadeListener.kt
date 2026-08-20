package io.waggle.waggleapiserver.domain.member.event

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class MemberCascadeListener(
    private val memberRepository: MemberRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        memberRepository.updateDeletedAtAndDeletedByByTeamIdAndDeletedAtIsNull(event.teamId, event.deletedBy)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        memberRepository.updateDeletedAtAndDeletedByByUserIdAndDeletedAtIsNull(event.userId)
    }
}
