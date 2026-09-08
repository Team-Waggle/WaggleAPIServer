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
import io.waggle.waggleapiserver.domain.post.QPost
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
        cursor: Long?,
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        sort: PostSort,
        size: Int,
    ): List<Post> =
        when (sort) {
            PostSort.NEWEST ->
                findOrderByColumn(q, positions, skills, size, cursor?.let { post.id.lt(it) }, post.id.desc())

            PostSort.OLDEST ->
                findOrderByColumn(q, positions, skills, size, cursor?.let { post.id.gt(it) }, post.id.asc())

            PostSort.MOST_VIEWED ->
                findOrderByColumn(
                    q,
                    positions,
                    skills,
                    size,
                    cursor?.let { afterViewCountCursor(it) },
                    post.viewCount.desc(),
                    post.id.desc(),
                )

            PostSort.MOST_LIKED -> findOrderByLikeCount(q, positions, skills, size, cursor)
            PostSort.DEADLINE_SOON -> findByDeadlineGroup(q, positions, skills, size, cursor)
        }

    // 하나의 CASE로 정렬하면 선두 키가 표현식이라 필터에 걸린 전체를 filesort 해야 해서 묶음별로 나눔
    private fun findByDeadlineGroup(
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        size: Int,
        cursor: Long?,
    ): List<Post> {
        val now = Instant.now()
        val startGroup =
            cursor?.let { deadlineGroupOf(it, now) ?: return emptyList() } ?: DeadlineGroup.EXPIRING

        val collected = mutableListOf<Post>()
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
        }
        return collected
    }

    // 커서 글이 삭제되면 이어붙일 지점이 없어 호출부가 빈 목록을 돌려줌
    private fun deadlineGroupOf(
        cursor: Long,
        now: Instant,
    ): DeadlineGroup? {
        val cursorPost =
            queryFactory.selectFrom(post).where(post.id.eq(cursor)).fetchOne() ?: return null
        val open =
            queryFactory
                .selectOne()
                .from(recruitment)
                .where(recruitment.postId.eq(cursor).and(recruitment.status.eq(RecruitmentStatus.RECRUITING)))
                .fetchFirst() != null

        val recruiting = open && (cursorPost.expiresAt == null || cursorPost.expiresAt!! > now)
        return when {
            !recruiting -> DeadlineGroup.CLOSED
            cursorPost.expiresAt == null -> DeadlineGroup.INDEFINITE
            else -> DeadlineGroup.EXPIRING
        }
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
        cursor: Long,
    ): BooleanExpression {
        if (group != DeadlineGroup.EXPIRING) {
            return post.id.lt(cursor)
        }
        val cursorPost = QPost("cursorPost")
        val cursorExpiresAt =
            JPAExpressions.select(cursorPost.expiresAt).from(cursorPost).where(cursorPost.id.eq(cursor))
        return post.expiresAt
            .gt(cursorExpiresAt)
            .or(post.expiresAt.eq(cursorExpiresAt).and(post.id.lt(cursor)))
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
        cursor: Long?,
    ): List<Post> {
        // 커서 글이 삭제되면 좋아요 수 서브쿼리가 0을 돌려줘 좋아요 있는 구간을 통째로 건너뜀
        if (cursor != null && !existsPost(cursor)) {
            return emptyList()
        }

        return queryFactory
            .selectFrom(post)
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
            .having(afterLikeCountCursor(cursor))
            .orderBy(OrderSpecifier(Order.DESC, like.count()), post.id.desc())
            .limit(size.toLong())
            .fetch()
    }

    private fun existsPost(id: Long): Boolean =
        queryFactory
            .selectOne()
            .from(post)
            .where(post.id.eq(id))
            .fetchFirst() != null

    // 복합 커서로 가면 CursorGetQuery, CursorResponse 계약이 전 API에서 바뀜
    private fun afterViewCountCursor(cursor: Long): BooleanExpression {
        val cursorPost = QPost("cursorPost")
        val cursorViewCount =
            JPAExpressions.select(cursorPost.viewCount).from(cursorPost).where(cursorPost.id.eq(cursor))
        return post.viewCount
            .lt(cursorViewCount)
            .or(post.viewCount.eq(cursorViewCount).and(post.id.lt(cursor)))
    }

    private fun afterLikeCountCursor(cursor: Long?): BooleanExpression? =
        cursor?.let {
            val cursorLikeCount =
                JPAExpressions
                    .select(like.count())
                    .from(like)
                    .where(
                        like.id.type
                            .eq(LikeType.POST)
                            .and(like.id.targetId.eq(it)),
                    )
            like
                .count()
                .lt(cursorLikeCount)
                .or(like.count().eq(cursorLikeCount).and(post.id.lt(it)))
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

    // 선언 순서가 곧 노출 순서임
    private enum class DeadlineGroup {
        EXPIRING,
        INDEFINITE,
        CLOSED,
    }
}
