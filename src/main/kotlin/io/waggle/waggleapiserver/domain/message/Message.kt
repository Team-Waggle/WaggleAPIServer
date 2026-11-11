package io.waggle.waggleapiserver.domain.message

import io.waggle.waggleapiserver.common.AuditingEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.Instant
import java.util.UUID

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_messages_sender_receiver", columnList = "sender_id, receiver_id"),
        Index(name = "idx_messages_receiver_sender", columnList = "receiver_id, sender_id"),
        Index(name = "idx_messages_receiver_read", columnList = "receiver_id, read_at"),
        // FULLTEXT 인덱스 ft_messages_content(content, ngram)는 JPA로 표현할 수 없어 V3 마이그레이션에만 있음
        // 메시지 검색의 MATCH ... AGAINST가 이 인덱스를 씀
    ],
)
class Message(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,
    @Column(name = "receiver_id", nullable = false)
    val receiverId: UUID,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
) : AuditingEntity() {
    @Column(name = "read_at")
    var readAt: Instant? = null
}
