package io.waggle.waggleapiserver.domain.message

import io.waggle.waggleapiserver.domain.message.dto.request.MessageSendRequest
import io.waggle.waggleapiserver.domain.message.service.MessageService
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.util.UUID

@MessageMapping("/message")
@Controller
class MessageStompController(
    private val messageService: MessageService,
) {
    @MessageMapping(".send")
    fun send(
        @Header("simpleSessionAttributes") attributes: Map<String, Any>,
        request: MessageSendRequest,
    ) {
        val senderId = attributes["userId"] as UUID
        val (receiverId, content) = request
        messageService.sendMessage(senderId, receiverId, content)
    }
}
