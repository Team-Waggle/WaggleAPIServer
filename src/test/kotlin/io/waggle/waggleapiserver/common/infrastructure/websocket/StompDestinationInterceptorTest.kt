package io.waggle.waggleapiserver.common.infrastructure.websocket

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

class StompDestinationInterceptorTest {
    private val interceptor = StompDestinationInterceptor()
    private val channel = MessageChannel { _, _ -> true }

    @Test
    fun `메시지 전송 경로로의 SEND는 통과`() {
        val message = stompMessage(StompCommand.SEND, "/app/message/send")

        assertThat(interceptor.preSend(message, channel)).isSameAs(message)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/user/00000000-0000-0000-0000-000000000000/queue/messages",
            "/app/message//send",
            "/queue/messages",
            "/topic/anything",
            "/app/other",
        ],
    )
    fun `그 외 경로로의 SEND는 drop`(destination: String) {
        assertThat(interceptor.preSend(stompMessage(StompCommand.SEND, destination), channel)).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/user/queue/messages", "/user/queue/errors"])
    fun `자기 수신함 구독은 통과`(destination: String) {
        val message = stompMessage(StompCommand.SUBSCRIBE, destination)

        assertThat(interceptor.preSend(message, channel)).isSameAs(message)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/queue/messages-user0123abcd",
            "/user/00000000-0000-0000-0000-000000000000/queue/messages",
            "/topic/anything",
        ],
    )
    fun `그 외 경로 구독은 drop`(destination: String) {
        assertThat(interceptor.preSend(stompMessage(StompCommand.SUBSCRIBE, destination), channel)).isNull()
    }

    @Test
    fun `목적지가 없는 SEND는 drop`() {
        assertThat(interceptor.preSend(stompMessage(StompCommand.SEND, destination = null), channel)).isNull()
    }

    @Test
    fun `SEND와 SUBSCRIBE가 아닌 프레임은 그대로 통과`() {
        val message = stompMessage(StompCommand.UNSUBSCRIBE, destination = null)

        assertThat(interceptor.preSend(message, channel)).isSameAs(message)
    }

    private fun stompMessage(
        command: StompCommand,
        destination: String?,
    ): Message<*> {
        val accessor = StompHeaderAccessor.create(command)
        accessor.destination = destination
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}
