package io.waggle.waggleapiserver.domain.conversation.repository

import io.waggle.waggleapiserver.domain.conversation.Conversation
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, Long> {
    @Query(
        """
        SELECT c FROM Conversation c
        WHERE c.userId = :userId
        AND (:cursor IS NULL OR c.lastMessageId < :cursor)
        ORDER BY c.lastMessageId DESC
        """,
    )
    fun findByUserId(
        userId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Conversation>

    @Query(
        """
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
        AND (:cursor IS NULL OR c.last_message_id < :cursor)
        ORDER BY c.last_message_id DESC
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun searchByUsernameOrContent(
        userId: UUID,
        q: String,
        cursor: Long?,
        limit: Int,
    ): List<Conversation>

    @Modifying
    @Query(
        """
        INSERT INTO conversations
            (user_id, partner_id, last_message_id, unread_count, created_at, updated_at)
        VALUES
            (:userId, :partnerId, :messageId, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE
            last_message_id = GREATEST(last_message_id, VALUES(last_message_id)),
            updated_at = UTC_TIMESTAMP(6)
        """,
        nativeQuery = true,
    )
    fun upsertLastMessage(
        userId: UUID,
        partnerId: UUID,
        messageId: Long,
    )

    @Modifying
    @Query(
        """
        INSERT INTO conversations
            (user_id, partner_id, last_message_id, unread_count, created_at, updated_at)
        VALUES
            (:userId, :partnerId, :messageId, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE
            last_message_id = GREATEST(last_message_id, VALUES(last_message_id)),
            unread_count = unread_count + 1,
            updated_at = UTC_TIMESTAMP(6)
        """,
        nativeQuery = true,
    )
    fun upsertLastMessageAndIncrementUnreadCount(
        userId: UUID,
        partnerId: UUID,
        messageId: Long,
    )

    @Modifying
    @Query(
        """
        UPDATE Conversation c
        SET c.unreadCount = 0,
            c.lastReadMessageId = :lastReadMessageId
        WHERE c.userId = :userId AND c.partnerId = :partnerId
        """,
    )
    fun markAsRead(
        userId: UUID,
        partnerId: UUID,
        lastReadMessageId: Long,
    ): Int
}
