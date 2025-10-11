package io.waggle.waggleapiserver.domain.project

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "projects",
    indexes = [Index(name = "idx_name", columnList = "name")],
)
class Project(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(unique = true, nullable = false) var name: String,
    @Column(nullable = false) var description: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User,
) : AuditingEntity() {
    fun update(
        name: String,
        description: String,
    ) {
        this.name = name
        this.description = description
    }

    fun isLeader(userId: UUID) = user.id == userId
}
