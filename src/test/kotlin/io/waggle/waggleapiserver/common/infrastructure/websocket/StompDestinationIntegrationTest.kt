package io.waggle.waggleapiserver.common.infrastructure.websocket

import com.fasterxml.jackson.databind.JsonNode
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.support.DatabaseCleaner
import io.waggle.waggleapiserver.support.TestcontainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

// 실제 WebSocket 연결로 사용자 목적지 처리기까지 거쳐야 위조 경로를 검증할 수 있어 RANDOM_PORT로 띄움
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "COOKIE_DOMAIN=localhost",
        "OAUTH2_REDIRECT_URI=http://localhost",
        "S3_BASE_URL=http://localhost",
        "S3_BUCKET=test-bucket",
        "JWT_SECRET=test-secret-test-secret-test-secret-test-secret",
        "GOOGLE_CLIENT_ID=test",
        "GOOGLE_CLIENT_SECRET=test",
        "KAKAO_CLIENT_ID=test",
        "KAKAO_CLIENT_SECRET=test",
    ],
)
@ActiveProfiles("mysql-test")
@Import(TestcontainersConfig::class)
class StompDestinationIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    private val sessions = mutableListOf<StompSession>()

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        sessions.forEach { if (it.isConnected) it.disconnect() }
        sessions.clear()
        databaseCleaner.clean()
    }

    @Test
    fun `사용자 목적지로 직접 보낸 위조 메시지는 전달되지 않는다`() {
        val attacker = createUser("attacker")
        val victim = createUser("victim")
        val victimInbox = subscribe(connect(victim), "/user/queue/messages")
        val attackerSession = connect(attacker)

        attackerSession.send("/user/${victim.id}/queue/messages", mapOf("content" to "FORGED"))
        attackerSession.send("/app/message/send", sendBody(victim.id, "REAL"))

        // 위조가 통과했다면 먼저 보냈으므로 먼저 도착함
        assertThat(victimInbox.poll(5, TimeUnit.SECONDS)?.get("content")?.asText()).isEqualTo("REAL")
        assertThat(victimInbox.poll(1, TimeUnit.SECONDS)).isNull()
    }

    @Test
    fun `슬래시를 겹친 전송 경로는 처리되지 않는다`() {
        val sender = createUser("sender")
        val receiver = createUser("receiver")
        val receiverInbox = subscribe(connect(receiver), "/user/queue/messages")
        val senderSession = connect(sender)

        senderSession.send("/app/message//send", sendBody(receiver.id, "DOUBLE_SLASH"))
        senderSession.send("/app/message/send", sendBody(receiver.id, "REAL"))

        assertThat(receiverInbox.poll(5, TimeUnit.SECONDS)?.get("content")?.asText()).isEqualTo("REAL")
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Long::class.java)).isEqualTo(1L)
    }

    @Test
    fun `전송이 실패하면 보낸 사람의 오류 큐로 알린다`() {
        val sender = createUser("sender")
        val senderSession = connect(sender)
        val errors = subscribe(senderSession, "/user/queue/errors")

        senderSession.send("/app/message/send", sendBody(sender.id, "to myself"))
        senderSession.send("/app/message/send", sendBody(UUID.randomUUID(), " "))

        // inbound 채널이 프레임을 병렬로 처리해 도착 순서는 보장되지 않음
        val received = listOfNotNull(errors.poll(5, TimeUnit.SECONDS), errors.poll(5, TimeUnit.SECONDS))
        assertThat(received.map { it.get("code").asText() }).containsOnly("INVALID_INPUT_VALUE").hasSize(2)
        assertThat(received.map { it.get("detail").asText() })
            .anyMatch { it == "Cannot send a message to yourself" }
            .anyMatch { it.startsWith("content:") }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Long::class.java)).isZero()
    }

    private fun createUser(name: String): User =
        userRepository.save(
            User(
                provider = "google",
                providerId = name,
                email = "$name@test.com",
                profileImageUrl = null,
            ).apply {
                username = name
                position = Position.BACKEND
            },
        )

    private fun connect(user: User): StompSession {
        val client =
            WebSocketStompClient(StandardWebSocketClient()).apply {
                messageConverter =
                    MappingJackson2MessageConverter()
            }
        val token = authService.issueWsToken(user.id).token
        return client
            .connectAsync("ws://127.0.0.1:$port/ws?token=$token", object : StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS)
            .also { sessions.add(it) }
    }

    // SUBSCRIBE는 응답 프레임이 없어 서버 등록 완료를 알 수 없으므로 잠시 대기함
    private fun subscribe(
        session: StompSession,
        destination: String,
    ): BlockingQueue<JsonNode> {
        val received = LinkedBlockingQueue<JsonNode>()
        session.subscribe(
            destination,
            object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): Type = JsonNode::class.java

                override fun handleFrame(
                    headers: StompHeaders,
                    payload: Any?,
                ) {
                    received.add(payload as JsonNode)
                }
            },
        )
        Thread.sleep(300)
        return received
    }

    private fun sendBody(
        receiverId: UUID,
        content: String,
    ) = mapOf("receiverId" to receiverId.toString(), "content" to content)
}
