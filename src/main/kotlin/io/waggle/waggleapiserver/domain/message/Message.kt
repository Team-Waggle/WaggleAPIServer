package io.waggle.waggleapiserver.domain.message

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "messages")
class Message(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,
    @Column(name = "receiver_id", nullable = false)
    val receiverId: UUID,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Column(name = "read_at")
    var readAt: Instant? = null
}
