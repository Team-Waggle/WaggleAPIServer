package io.waggle.waggleapiserver.domain.conversation.repository

import io.waggle.waggleapiserver.domain.conversation.Conversation
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
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
class ConversationRepositoryTest
    @Autowired
    constructor(
        private val conversationRepository: ConversationRepository,
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
        private val entityManager: EntityManager,
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

        @BeforeEach
        fun setUp() {
            cleanup()

            me = userRepository.save(createUser("me"))
            partner1 = userRepository.save(createUser("alice"))

            me.username = "me"
            me.position = Position.BACKEND
            partner1.username = "alice"
            partner1.position = Position.FRONTEND

            userRepository.saveAll(listOf(me, partner1))
        }

        @AfterEach
        fun cleanup() {
            conversationRepository.deleteAll()
            messageRepository.deleteAll()
            userRepository.deleteAll()
        }

        @Test
        fun `EXPLAIN - searchByUsernameOrContent 실행 계획`() {
            val message1 =
                messageRepository.save(
                    Message(
                        senderId = me.id,
                        receiverId = partner1.id,
                        content = "프로젝트 회의",
                    ),
                )
            conversationRepository.save(
                Conversation(
                    userId = me.id,
                    partnerId = partner1.id,
                    lastMessageId = message1.id,
                ),
            )

            val sql =
                """
                EXPLAIN
                SELECT c.* FROM conversations c
                JOIN users u ON u.id = c.partner_id
                WHERE c.user_id = :userId
                AND (
                    u.username LIKE CONCAT('%', :q, '%')
                    OR c.partner_id IN (
                        SELECT IF(m.sender_id = :userId, m.receiver_id, m.sender_id)
                        FROM messages m
                        WHERE (m.sender_id = :userId OR m.receiver_id = :userId)
                        AND MATCH(m.content) AGAINST(:q IN BOOLEAN MODE)
                    )
                )
                AND c.last_message_id < :cursor
                ORDER BY c.last_message_id DESC
                LIMIT :lim
                """.trimIndent()

            val results =
                entityManager
                    .createNativeQuery(sql)
                    .setParameter("userId", me.id)
                    .setParameter("q", "회의")
                    .setParameter("cursor", Long.MAX_VALUE)
                    .setParameter("lim", 20)
                    .resultList

            println("\n===== EXPLAIN searchByUsernameOrContent =====")
            println(
                String.format(
                    "%-5s %-15s %-25s %-10s %-40s %-10s %-10s %-10s %-10s %-30s",
                    "id",
                    "select_type",
                    "table",
                    "type",
                    "possible_keys",
                    "key",
                    "key_len",
                    "ref",
                    "rows",
                    "Extra",
                ),
            )
            println("-".repeat(170))
            for (row in results) {
                val cols = row as Array<*>
                println(
                    String.format(
                        "%-5s %-15s %-25s %-10s %-40s %-10s %-10s %-10s %-10s %-30s",
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4] ?: "",
                        cols[5] ?: "",
                        cols[6] ?: "",
                        cols[7] ?: "",
                        cols[8] ?: "",
                        cols[9] ?: "",
                    ),
                )
            }
            println("=".repeat(170))
        }

        @Test
        fun `EXPLAIN - searchByContent 실행 계획`() {
            messageRepository.save(
                Message(
                    senderId = me.id,
                    receiverId = partner1.id,
                    content = "프로젝트 회의",
                ),
            )

            val sql =
                """
                EXPLAIN
                SELECT m.* FROM messages m
                INNER JOIN (
                    SELECT MAX(m2.id) AS max_id
                    FROM messages m2
                    WHERE (m2.sender_id = :userId OR m2.receiver_id = :userId)
                    AND MATCH(m2.content) AGAINST(:q IN BOOLEAN MODE)
                    GROUP BY IF(m2.sender_id = :userId, m2.receiver_id, m2.sender_id)
                ) latest ON m.id = latest.max_id
                ORDER BY m.id DESC
                LIMIT :lim
                """.trimIndent()

            val results =
                entityManager
                    .createNativeQuery(sql)
                    .setParameter("userId", me.id)
                    .setParameter("q", "회의")
                    .setParameter("lim", 20)
                    .resultList

            println("\n===== EXPLAIN searchByContent =====")
            println(
                String.format(
                    "%-5s %-15s %-25s %-10s %-40s %-10s %-10s %-10s %-10s %-30s",
                    "id",
                    "select_type",
                    "table",
                    "type",
                    "possible_keys",
                    "key",
                    "key_len",
                    "ref",
                    "rows",
                    "Extra",
                ),
            )
            println("-".repeat(170))
            for (row in results) {
                val cols = row as Array<*>
                println(
                    String.format(
                        "%-5s %-15s %-25s %-10s %-40s %-10s %-10s %-10s %-10s %-30s",
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4] ?: "",
                        cols[5] ?: "",
                        cols[6] ?: "",
                        cols[7] ?: "",
                        cols[8] ?: "",
                        cols[9] ?: "",
                    ),
                )
            }
            println("=".repeat(170))
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
