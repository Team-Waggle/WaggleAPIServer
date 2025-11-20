package io.waggle.waggleapiserver.domain.message.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.data.redis.connection.Message as RedisMessage

@Component
class MessageSubscriber(
    private val objectMapper: ObjectMapper,
    private val messageRepository: MessageRepository,
    private val messagingTemplate: SimpMessagingTemplate,
) : MessageListener {
    override fun onMessage(
        redisMessage: RedisMessage,
        pattern: ByteArray?,
    ) {
        val payload = String(redisMessage.body)
        val event = objectMapper.readValue(payload, MessageEvent::class.java)

        val message = messageRepository.findById(event.messageId).orElse(null) ?: return

        val response = MessageResponse.from(message)

        messagingTemplate.convertAndSendToUser(
            event.receiverId.toString(),
            "/queue/messages",
            response,
        )
    }
}
