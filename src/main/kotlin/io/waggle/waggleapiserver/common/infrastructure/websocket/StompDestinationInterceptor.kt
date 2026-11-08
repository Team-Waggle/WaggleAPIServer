package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.waggle.waggleapiserver.common.util.logger
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

// 클라이언트 SEND가 /user/{id}/... 로 가면 사용자 목적지 처리기가 그 사용자에게 그대로 전달함
// 서버를 거치지 않은 위조 메시지가 되므로 허용 목록 밖은 모두 버림
@Component
class StompDestinationInterceptor : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        val allowedDestinations =
            when (accessor.command) {
                StompCommand.SEND -> SEND_DESTINATIONS
                StompCommand.SUBSCRIBE -> SUBSCRIBE_DESTINATIONS
                else -> return message
            }
        if (accessor.destination in allowedDestinations) return message

        logger.warn("Rejected STOMP {} to {} from {}", accessor.command, accessor.destination, accessor.user?.name)
        return null
    }

    companion object {
        // 정확히 일치해야 함
        // 핸들러 매칭은 /app/message//send도 통과시켜 문자열 비교인 레이트 리밋을 우회함
        private val SEND_DESTINATIONS = setOf("/app/message/send")
        private val SUBSCRIBE_DESTINATIONS = setOf("/user/queue/messages", "/user/queue/errors")
    }
}
