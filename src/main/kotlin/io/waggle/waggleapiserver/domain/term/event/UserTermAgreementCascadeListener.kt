package io.waggle.waggleapiserver.domain.term.event

import io.waggle.waggleapiserver.domain.term.repository.UserTermAgreementRepository
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class UserTermAgreementCascadeListener(
    private val userTermAgreementRepository: UserTermAgreementRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        userTermAgreementRepository.deleteByUserId(event.userId)
    }
}
