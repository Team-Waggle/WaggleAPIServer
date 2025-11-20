package io.waggle.waggleapiserver.domain.message

import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.service.MessageService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/messages")
class MessageController(
    val messageService: MessageService,
) {
    @GetMapping("/{partnerId}")
    fun getMessageHistory(
        @PathVariable partnerId: UUID,
        @CurrentUser user: User,
    ): List<MessageResponse> = messageService.getMessageHistory(partnerId, user)
}
