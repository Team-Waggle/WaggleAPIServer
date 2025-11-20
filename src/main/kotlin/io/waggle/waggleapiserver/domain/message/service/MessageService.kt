package io.waggle.waggleapiserver.domain.message.service

import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.message.adapter.MessageEvent
import io.waggle.waggleapiserver.domain.message.adapter.MessagePublisher
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MessageService(
    private val messagePublisher: MessagePublisher,
    private val messageRepository: MessageRepository,
) {
    @Transactional
    fun sendMessage(
        senderId: UUID,
        receiverId: UUID,
        content: String,
    ) {
        val message =
            Message(
                senderId = senderId,
                receiverId = receiverId,
                content = content,
            )
        val savedMessage = messageRepository.save(message)

        val event =
            MessageEvent(
                messageId = savedMessage.id,
                receiverId = receiverId,
            )
        messagePublisher.publish(event)
    }

    @Transactional(readOnly = true)
    fun getMessageHistory(
        partnerId: UUID,
        user: User,
    ): List<MessageResponse> = messageRepository.findMessageHistory(user.id, partnerId).map { MessageResponse.from(it) }
}
