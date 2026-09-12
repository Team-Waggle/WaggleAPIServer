package io.waggle.waggleapiserver.domain.post.repository

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.QLike.like
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.post.QPost.post
import io.waggle.waggleapiserver.domain.recruitment.QRecruitment.recruitment
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import java.time.Instant

class PostRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : PostRepositoryCustom {
    override fun findWithFilter(
        cursor: PostCursor?,
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        sort: PostSort,
        size: Int,
    ): List<PostWithCursor> =
        when (sort) {
            PostSort.NEWEST ->
                findOrderByColumn(
                    q,
                    positions,
                    skills,
                    size,
                    cursor?.let { post.id.lt(it.id) },
                    post.id.desc(),
                ).map { PostWithCursor(it, PostCursor.Id(it.id)) }

            PostSort.OLDEST ->
                findOrderByColumn(
                    q,
                    positions,
                    skills,
                    size,
                    cursor?.let { post.id.gt(it.id) },
                    post.id.asc(),
                ).map { PostWithCursor(it, PostCursor.Id(it.id)) }

            PostSort.MOST_VIEWED ->
                findOrderByColumn(
                    q,
                    positions,
                    skills,
                    size,
                    (cursor as PostCursor.ViewCount?)?.let { afterViewCountCursor(it) },
                    post.viewCount.desc(),
                    post.id.desc(),
                ).map { PostWithCursor(it, PostCursor.ViewCount(it.viewCount, it.id)) }

            PostSort.MOST_LIKED ->
                findOrderByLikeCount(q, positions, skills, size, cursor as PostCursor.LikeCount?)

            PostSort.DEADLINE_SOON ->
                findByDeadlineGroup(q, positions, skills, size, cursor as PostCursor.Deadline?)
        }

    // 하나의 CASE로 정렬하면 선두 키가 표현식이라 필터에 걸린 전체를 filesort 해야 해서 묶음별로 나눔
    private fun findByDeadlineGroup(
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        size: Int,
        cursor: PostCursor.Deadline?,
    ): List<PostWithCursor> {
        val now = Instant.now()
        val startGroup = cursor?.group ?: DeadlineGroup.EXPIRING

        val collected = mutableListOf<PostWithCursor>()
        for (group in DeadlineGroup.entries.drop(startGroup.ordinal)) {
            val remaining = size - collected.size
            if (remaining <= 0) break

            collected +=
                queryFactory
                    .selectFrom(post)
                    .where(
                        deadlineGroup(group, now),
                        cursor?.takeIf { group == startGroup }?.let { afterDeadlineCursor(group, it) },
                        titleContains(q),
                        recruitedForPosition(positions),
                        recruitedWithSkill(skills),
                    ).orderBy(*deadlineOrderBy(group))
                    .limit(remaining.toLong())
                    .fetch()
                    .map { PostWithCursor(it, PostCursor.Deadline(group, it.expiresAt, it.id)) }
        }
        return collected
    }

    private fun deadlineGroup(
        group: DeadlineGroup,
        now: Instant,
    ): BooleanExpression {
        val recruiting =
            post.expiresAt.isNull
                .or(post.expiresAt.gt(now))
                .and(hasOpenRecruitment())
        return when (group) {
            DeadlineGroup.EXPIRING -> recruiting.and(post.expiresAt.isNotNull)
            DeadlineGroup.INDEFINITE -> recruiting.and(post.expiresAt.isNull)
            DeadlineGroup.CLOSED -> recruiting.not()
        }
    }

    private fun afterDeadlineCursor(
        group: DeadlineGroup,
        cursor: PostCursor.Deadline,
    ): BooleanExpression {
        if (group != DeadlineGroup.EXPIRING) {
            return post.id.lt(cursor.id)
        }
        return post.expiresAt
            .gt(cursor.expiresAt)
            .or(post.expiresAt.eq(cursor.expiresAt).and(post.id.lt(cursor.id)))
    }

    private fun deadlineOrderBy(group: DeadlineGroup): Array<OrderSpecifier<*>> =
        if (group == DeadlineGroup.EXPIRING) {
            arrayOf(post.expiresAt.asc(), post.id.desc())
        } else {
            arrayOf(post.id.desc())
        }

    private fun hasOpenRecruitment(): BooleanExpression =
        hasRecruitment(recruitment.status.eq(RecruitmentStatus.RECRUITING))

    private fun findOrderByColumn(
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        size: Int,
        afterCursor: BooleanExpression?,
        vararg orderBy: OrderSpecifier<*>,
    ): List<Post> =
        queryFactory
            .selectFrom(post)
            .where(
                afterCursor,
                titleContains(q),
                recruitedForPosition(positions),
                recruitedWithSkill(skills),
            ).orderBy(*orderBy)
            .limit(size.toLong())
            .fetch()

    private fun findOrderByLikeCount(
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        size: Int,
        cursor: PostCursor.LikeCount?,
    ): List<PostWithCursor> {
        val likeCount = like.count()
        return queryFactory
            .select(post, likeCount)
            .from(post)
            .leftJoin(like)
            .on(
                like.id.type
                    .eq(LikeType.POST)
                    .and(like.id.targetId.eq(post.id)),
            ).where(
                titleContains(q),
                recruitedForPosition(positions),
                recruitedWithSkill(skills),
            ).groupBy(post)
            .having(cursor?.let { afterLikeCountCursor(it) })
            .orderBy(OrderSpecifier(Order.DESC, likeCount), post.id.desc())
            .limit(size.toLong())
            .fetch()
            .map {
                val found = it.get(post)!!
                PostWithCursor(found, PostCursor.LikeCount(it.get(likeCount)!!, found.id))
            }
    }

    private fun afterViewCountCursor(cursor: PostCursor.ViewCount): BooleanExpression =
        post.viewCount
            .lt(cursor.viewCount)
            .or(post.viewCount.eq(cursor.viewCount).and(post.id.lt(cursor.id)))

    private fun afterLikeCountCursor(cursor: PostCursor.LikeCount): BooleanExpression =
        like
            .count()
            .lt(cursor.likeCount)
            .or(like.count().eq(cursor.likeCount).and(post.id.lt(cursor.id)))

    private fun titleContains(q: String?): BooleanExpression? = q?.let { post.title.contains(it) }

    private fun recruitedForPosition(positions: Set<Position>): BooleanExpression? =
        positions.takeIf { it.isNotEmpty() }?.let { hasRecruitment(recruitment.position.`in`(it)) }

    private fun recruitedWithSkill(skills: Set<Skill>): BooleanExpression? =
        skills.takeIf { it.isNotEmpty() }?.let { hasRecruitment(recruitment.skills.any().`in`(it)) }

    private fun hasRecruitment(condition: BooleanExpression): BooleanExpression =
        post.id.`in`(
            JPAExpressions.select(recruitment.postId).from(recruitment).where(condition),
        )
}
