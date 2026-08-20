package io.waggle.waggleapiserver.domain.auth.event

import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AuthCascadeListener(
    private val authService: AuthService,
) {
    // refresh token은 Redis(트랜잭션 밖) → 탈퇴가 실제 커밋된 뒤에만 제거
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        authService.deleteRefreshToken(event.userId)
    }
}
