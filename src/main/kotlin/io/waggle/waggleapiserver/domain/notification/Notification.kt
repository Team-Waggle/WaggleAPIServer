package io.waggle.waggleapiserver.domain.notification

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(
            name = "idx_notifications_user_read_created",
            columnList = "user_id, read_at, created_at DESC",
        ),
        Index(name = "idx_notifications_team", columnList = "team_id"),
        Index(name = "idx_notifications_post", columnList = "post_id"),
    ],
)
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(32)")
    val type: NotificationType,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Convert(converter = NotificationMetadataConverter::class)
    @Column(columnDefinition = "JSON")
    val metadata: Map<String, Any?> = emptyMap(),
) {
    @Column(name = "read_at")
    var readAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    // metadata(JSON)의 teamId/postId를 노출하는 generated STORED 컬럼(읽기 전용).
    // 값 계산·저장은 DB가 담당하므로 insertable/updatable = false. cascade 삭제 및 조회 인덱스 대상.
    @Column(name = "team_id", insertable = false, updatable = false, columnDefinition = "BIGINT")
    val teamId: Long? = null

    @Column(name = "post_id", insertable = false, updatable = false, columnDefinition = "BIGINT")
    val postId: Long? = null
}
