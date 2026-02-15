package io.waggle.waggleapiserver.domain.team

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Access(AccessType.FIELD)
@Entity
@Table(
    name = "teams",
    indexes = [Index(name = "idx_teams_name", columnList = "name")],
)
class Team(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true, nullable = false)
    var name: String,
    @Column(nullable = false)
    var description: String,
    @Column(name = "profile_image_url")
    var profileImageUrl: String? = null,
    @Column(name = "leader_id", nullable = false)
    var leaderId: UUID,
    @Column(name = "creator_id", nullable = false, updatable = false)
    val creatorId: UUID,
) : AuditingEntity(),
    Bookmarkable {
    override val targetId: Long
        get() = id
    override val type: BookmarkType
        get() = BookmarkType.TEAM

    fun update(
        name: String,
        description: String,
        profileImageUrl: String?,
    ) {
        this.name = name
        this.description = description
        this.profileImageUrl = profileImageUrl
    }

    fun isLeader(userId: UUID) = leaderId == userId
}
