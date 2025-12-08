package io.waggle.waggleapiserver.domain.bookmark

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class BookmarkId(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val type: BookmarkType,
) : Serializable

@Entity
@Table(
    name = "bookmarks",
    indexes = [
        Index(
            name = "idx_bookmarks_type_target",
            columnList = "type, target_id",
        ),
    ],
)
class Bookmark(
    @EmbeddedId
    val id: BookmarkId,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    val targetId: Long get() = id.targetId
    val type: BookmarkType get() = id.type
}

enum class BookmarkType {
    POST,
    PROJECT,
}
