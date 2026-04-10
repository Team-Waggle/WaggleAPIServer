package io.waggle.waggleapiserver.domain.conversation.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.conversation.Conversation
import io.waggle.waggleapiserver.domain.conversation.repository.ConversationRepository
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataJpaTest
@Testcontainers
@ActiveProfiles("mysql-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(ConversationService::class)
class ConversationServiceTest
    @Autowired
    constructor(
        private val conversationService: ConversationService,
        private val conversationRepository: ConversationRepository,
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
    ) {
        companion object {
            @Container
            @ServiceConnection
            val mysql =
                MySQLContainer("mysql:8.0").apply {
                    withDatabaseName("waggle")
                    withCommand("--ngram-token-size=2")
                }
        }

        private lateinit var me: User
        private lateinit var partner1: User
        private lateinit var partner2: User
        private lateinit var partner3: User

        @BeforeEach
        fun setUp() {
            cleanup()

            me = userRepository.save(createUser("me"))
            partner1 = userRepository.save(createUser("alice"))
            partner2 = userRepository.save(createUser("bob"))
            partner3 = userRepository.save(createUser("charlie"))

            me.username = "me"
            me.position = Position.BACKEND
            partner1.username = "alice"
            partner1.position = Position.FRONTEND
            partner2.username = "bob"
            partner2.position = Position.DESIGNER
            partner3.username = "charlie"
            partner3.position = Position.BACKEND

            userRepository.saveAll(listOf(me, partner1, partner2, partner3))
        }

        @AfterEach
        fun cleanup() {
            conversationRepository.deleteAll()
            messageRepository.deleteAll()
            userRepository.deleteAll()
        }

        @Test
        fun `q가 null이면 전체 대화방 목록 조회`() {
            val message1 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "hello"),
                )
            val message2 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner2.id, content = "hi"),
                )

            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner1.id, lastMessageId = message1.id),
            )
            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner2.id, lastMessageId = message2.id),
            )

            val result = conversationService.getConversations(null, CursorGetQuery(null, 20), me)

            assertThat(result.data).hasSize(2)
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `username으로 검색하면 해당 대화방만 반환`() {
            val message1 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "hello"),
                )
            val message2 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner2.id, content = "hello"),
                )

            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner1.id, lastMessageId = message1.id),
            )
            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner2.id, lastMessageId = message2.id),
            )

            val result = conversationService.getConversations("ali", CursorGetQuery(null, 20), me)

            assertThat(result.data).hasSize(1)
            assertThat(result.data[0].partner.username).isEqualTo("alice")
        }

        @Test
        fun `content로 검색하면 매칭 메시지가 lastMessage로 설정`() {
            val message1 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "안녕하세요"),
                )
            val message2 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "프로젝트 회의합시다"),
                )

            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner1.id, lastMessageId = message2.id),
            )

            val result = conversationService.getConversations("안녕", CursorGetQuery(null, 20), me)

            assertThat(result.data).hasSize(1)
            assertThat(result.data[0].lastMessage.messageId).isEqualTo(message1.id)
        }

        @Test
        fun `username과 content 동시 매칭 시 중복 없음`() {
            val message =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "alice에게 보내는 메시지"),
                )

            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner1.id, lastMessageId = message.id),
            )

            val result = conversationService.getConversations("alice", CursorGetQuery(null, 20), me)

            assertThat(result.data).hasSize(1)
        }

        @Test
        fun `커서 페이지네이션 - 중복 및 누락 없음`() {
            val message1 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "프로젝트 논의"),
                )
            val message2 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner2.id, content = "프로젝트 회의"),
                )
            val message3 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner3.id, content = "프로젝트 미팅"),
                )

            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner1.id, lastMessageId = message1.id),
            )
            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner2.id, lastMessageId = message2.id),
            )
            conversationRepository.save(
                Conversation(userId = me.id, partnerId = partner3.id, lastMessageId = message3.id),
            )

            // 첫 페이지: size 2
            val page1 = conversationService.getConversations("프로젝트", CursorGetQuery(null, 2), me)
            assertThat(page1.data).hasSize(2)
            assertThat(page1.hasNext).isTrue()

            // 두 번째 페이지
            val page2 =
                conversationService.getConversations("프로젝트", CursorGetQuery(page1.nextCursor, 2), me)
            assertThat(page2.data).hasSize(1)
            assertThat(page2.hasNext).isFalse()

            // 중복 없음
            val allPartnerIds = (page1.data + page2.data).map { it.partner.userId }
            assertThat(allPartnerIds).doesNotHaveDuplicates()

            // 누락 없음
            assertThat(allPartnerIds).containsExactlyInAnyOrder(partner1.id, partner2.id, partner3.id)
        }

        @Test
        fun `searchByContent - 파트너별 최근 매칭 메시지 조회`() {
            messageRepository.save(
                Message(senderId = me.id, receiverId = partner1.id, content = "첫 번째 회의"),
            )
            val message2 =
                messageRepository.save(
                    Message(senderId = me.id, receiverId = partner1.id, content = "두 번째 회의"),
                )

            val result = messageRepository.searchByContent(me.id, "회의", 20)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(message2.id)
        }

        private fun createUser(name: String): User =
            User(
                provider = "google",
                providerId = name,
                email = "$name@test.com",
                profileImageUrl = null,
                role = UserRole.USER,
            )
    }
