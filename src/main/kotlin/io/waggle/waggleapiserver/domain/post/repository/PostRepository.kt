package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.domain.post.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, Long> {
    @Query(
        """
        SELECT p FROM Post p
        WHERE (:q IS NULL OR p.title LIKE CONCAT('%', :q, '%'))
    """,
    )
    fun findWithFilter(
        q: String?,
        pageable: Pageable,
    ): Page<Post>

    fun findByIdInOrderByCreatedAtDesc(ids: List<Long>): List<Post>
}
