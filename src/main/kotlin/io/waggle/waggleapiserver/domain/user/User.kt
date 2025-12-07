package io.waggle.waggleapiserver.domain.user

import com.github.f4b6a3.uuid.UuidCreator
import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
    indexes = [Index(name = "idx_users_email", columnList = "email")],
)
class User(
    @Id
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    @Column(nullable = false)
    val provider: String,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
    @Column(nullable = false)
    val email: String,
    @Column(name = "profile_image_url")
    var profileImageUrl: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(10)")
    var role: UserRole = UserRole.USER,
) : AuditingEntity() {
    var username: String? = null

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    var position: Position? = null

    @ElementCollection
    @CollectionTable(name = "user_skills", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, columnDefinition = "VARCHAR(50)")
    val skills: MutableSet<Skill> = mutableSetOf()

    @Column(columnDefinition = "VARCHAR(1000)")
    var bio: String? = null

    fun update(
        username: String,
        position: Position,
        bio: String?,
    ) {
        this.username = username
        this.position = position
        this.bio = bio
    }

    fun isProfileComplete(): Boolean = this.username != null && this.position != null && this.skills.isNotEmpty()
}
