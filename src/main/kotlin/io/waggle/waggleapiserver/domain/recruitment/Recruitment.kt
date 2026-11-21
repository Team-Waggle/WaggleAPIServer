package io.waggle.waggleapiserver.domain.recruitment

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "recruitments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "position"])],
    indexes = [Index(name = "idx_recruitments_post", columnList = "post_id")],
)
class Recruitment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    val position: Position,
    @Column(nullable = false)
    var count: Int,
    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: Long,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recruitment_skills", joinColumns = [JoinColumn(name = "recruitment_id")])
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, columnDefinition = "VARCHAR(30)")
    val skills: MutableSet<Skill> = mutableSetOf(),
) {
    // 마감은 되돌릴 수 없어 close만 상태를 바꿀 수 있음
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    final var status: RecruitmentStatus = RecruitmentStatus.RECRUITING
        private set

    // 승인 처리의 native UPDATE만 이 컬럼을 씀
    // JPA가 쓰면 저장 시 메모리의 옛 값으로 덮어씀 (CLAUDE.md 2절)
    // 탈퇴한 승인자도 뽑은 인원으로 세어 줄이는 경로가 없음
    // 승인을 취소하는 기능이 생기면 그 경로에서 함께 줄여야 함
    @Column(name = "approved_count", nullable = false, insertable = false, updatable = false)
    val approvedCount: Int = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant

    fun isRecruiting(): Boolean = status == RecruitmentStatus.RECRUITING

    fun update(
        count: Int,
        skills: Set<Skill>,
    ) {
        this.count = count
        this.skills.clear()
        this.skills.addAll(skills)
    }

    fun close() {
        status = RecruitmentStatus.CLOSED
    }

    // 승인과 모집 인원 수정이 모두 이 규칙을 지나야 다 찬 직무가 모집 중으로 남지 않음
    // approvedCount 필드는 native UPDATE로만 바뀌어 승인 경로는 이번 승인을 더한 값을 넘김
    fun closeIfFull(approvedCount: Int) {
        if (approvedCount > count) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Approved count $approvedCount exceeds recruitment count $count for $position",
            )
        }
        if (approvedCount == count) {
            close()
        }
    }
}
