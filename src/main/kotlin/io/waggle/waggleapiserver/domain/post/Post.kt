package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
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
    name = "posts",
    indexes = [
        Index(name = "idx_posts_title", columnList = "title"),
        Index(name = "idx_posts_team", columnList = "team_id"),
        Index(name = "idx_posts_user", columnList = "user_id"),
    ],
)
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "team_id", nullable = false, updatable = false)
    val teamId: Long,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
) : AuditingEntity(),
    Bookmarkable {
    // 스케줄러의 native UPDATE만 이 컬럼을 씀
    @Column(name = "view_count", nullable = false, insertable = false, updatable = false)
    val viewCount: Long = 0

    override val targetId: Long
        get() = id
    override val type: BookmarkType
        get() = BookmarkType.POST

    // NULL은 무기한 모집이라 만료가 아니고, 배타적 경계라 만료 시각에 도달하면 만료됨
    val isExpired: Boolean
        get() = expiresAt?.let { it <= Instant.now() } == true

    fun update(
        title: String,
        content: String,
        expiresAt: Instant?,
    ) {
        // 값이 바뀔 때만 검사함. 이미 마감된 글도 기존 마감일을 그대로 실어 보내며 본문을 고칠 수 있어야 함
        if (expiresAt != this.expiresAt && expiresAt != null && Deadline.isPast(expiresAt)) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT_VALUE,
                "Deadline cannot be moved to the past: $id",
            )
        }

        this.title = title
        this.content = content
        this.expiresAt = expiresAt
    }

    // 마감일 상한을 오늘로 둠. 어제로 당기면 오늘 들어온 지원이 마감일 이후 기록이 됨.
    // 이미 지난 기한은 그대로 둬 마감일이 미래로 밀리지 않게 함
    fun limitDeadlineToToday() {
        val today = Deadline.todayExpiresAt()
        val current = expiresAt
        if (current == null || current > today) {
            expiresAt = today
        }
    }

    fun checkNotExpired() {
        if (isExpired) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Post is closed: $id")
        }
    }

    fun checkOwnership(currentUserId: UUID) {
        if (userId != currentUserId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Not the owner of the post")
        }
    }
}
