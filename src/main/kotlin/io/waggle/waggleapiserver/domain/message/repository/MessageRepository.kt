package io.waggle.waggleapiserver.domain.message.repository

import io.waggle.waggleapiserver.domain.message.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageRepository : JpaRepository<Message, Long> {
    @Query(
        """
        SELECT m FROM Message m
        WHERE (m.senderId = :userId AND m.receiverId = :partnerId)
        OR (m.senderId = :partnerId AND m.senderId = :userId)
        ORDER BY m.createdAt
    """,
    )
    fun findMessageHistory(
        userId: UUID,
        partnerId: UUID,
    ): List<Message>
}
