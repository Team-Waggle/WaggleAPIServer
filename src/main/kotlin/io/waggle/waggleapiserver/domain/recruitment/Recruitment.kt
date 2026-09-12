package io.waggle.waggleapiserver.domain.recruitment

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
    // allOpen이 엔티티를 열어 private setter는 컴파일이 안 되고, final을 붙이면 Hibernate가 프록시를 못 만들어 HHH000305 경고를 냄
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    var status: RecruitmentStatus = RecruitmentStatus.RECRUITING
        protected set

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
}
