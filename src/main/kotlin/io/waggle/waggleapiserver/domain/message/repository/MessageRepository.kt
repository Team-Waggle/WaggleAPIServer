package io.waggle.waggleapiserver.domain.message.repository

import io.waggle.waggleapiserver.domain.message.Message
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageRepository : JpaRepository<Message, Long> {
    @Query(
        """
        SELECT m FROM Message m
        WHERE ((m.senderId = :userId AND m.receiverId = :partnerId)
            OR (m.senderId = :partnerId AND m.receiverId = :userId))
        AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
    """,
    )
    fun findMessageHistoryByCursor(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Message>
}
