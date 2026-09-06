package io.waggle.waggleapiserver.domain.post.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.post.QPost.post
import io.waggle.waggleapiserver.domain.recruitment.QRecruitment.recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill

class PostRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : PostRepositoryCustom {
    override fun findWithFilter(
        cursor: Long?,
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        sort: PostSort,
        size: Int,
    ): List<Post> =
        queryFactory
            .selectFrom(post)
            .where(
                afterCursor(cursor, sort),
                titleContains(q),
                recruitedForPosition(positions),
                recruitedWithSkill(skills),
            ).orderBy(orderBy(sort))
            .limit(size.toLong())
            .fetch()

    private fun afterCursor(
        cursor: Long?,
        sort: PostSort,
    ): BooleanExpression? =
        cursor?.let {
            when (sort) {
                PostSort.NEWEST -> post.id.lt(it)
                PostSort.OLDEST -> post.id.gt(it)
            }
        }

    private fun titleContains(q: String?): BooleanExpression? = q?.let { post.title.contains(it) }

    private fun recruitedForPosition(positions: Set<Position>): BooleanExpression? =
        positions.takeIf { it.isNotEmpty() }?.let { hasRecruitment(recruitment.position.`in`(it)) }

    private fun recruitedWithSkill(skills: Set<Skill>): BooleanExpression? =
        skills.takeIf { it.isNotEmpty() }?.let { hasRecruitment(recruitment.skills.any().`in`(it)) }

    private fun hasRecruitment(condition: BooleanExpression): BooleanExpression =
        post.id.`in`(
            JPAExpressions.select(recruitment.postId).from(recruitment).where(condition),
        )

    private fun orderBy(sort: PostSort): OrderSpecifier<*> =
        when (sort) {
            PostSort.NEWEST -> post.id.desc()
            PostSort.OLDEST -> post.id.asc()
        }
}
