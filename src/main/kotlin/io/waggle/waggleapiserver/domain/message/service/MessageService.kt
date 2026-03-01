package io.waggle.waggleapiserver.domain.message.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.message.adapter.MessageEvent
import io.waggle.waggleapiserver.domain.message.adapter.MessagePublisher
import io.waggle.waggleapiserver.domain.message.dto.request.MessageSendRequest
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MessageService(
    private val messagePublisher: MessagePublisher,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun sendMessage(
        senderId: UUID,
        request: MessageSendRequest,
    ) {
        val (receiverId, content) = request

        if (!userRepository.existsById(receiverId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Receiver not found: $receiverId")
        }

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
        query: CursorGetQuery,
    ): CursorResponse<MessageResponse> {
        val (cursor, size) = query

        val messages =
            messageRepository.findMessageHistoryByCursor(
                user.id,
                partnerId,
                cursor,
                PageRequest.of(0, size + 1),
            )

        val hasNext = messages.size > size
        val data =
            (if (hasNext) messages.dropLast(1) else messages)
                .map { MessageResponse.from(it) }
        val nextCursor = if (hasNext) data.last().messageId else null

        return CursorResponse(
            data = data,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }
}
