package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.post.dto.response.BookmarkedPostResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun bookmark(
        type: BookmarkType,
        targetId: Long,
        user: User,
    ): Boolean {
        checkBookmarkableTarget(type, targetId)
        return bookmarkRepository.insertIfAbsent(type, targetId, user.id) == 1
    }

    @Transactional
    fun unbookmark(
        type: BookmarkType,
        targetId: Long,
        user: User,
    ) {
        checkBookmarkableTarget(type, targetId)
        bookmarkRepository.deleteIfPresent(type, targetId, user.id)
    }

    fun getUserBookmarkables(
        type: BookmarkType,
        user: User,
    ): List<BookmarkResponse> {
        val targetIds =
            bookmarkRepository
                .findByIdUserIdAndIdType(user.id, type)
                .map { it.targetId }

        return when (type) {
            BookmarkType.POST -> {
                val posts = postRepository.findByIdInOrderByIdDesc(targetIds)
                val authorIds = posts.map { it.userId }.distinct()
                val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

                val postIds = posts.map { it.id }
                val recruitmentsByPostId =
                    recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

                posts.map { post ->
                    val author =
                        authorById[post.userId]
                            ?: throw BusinessException(
                                ErrorCode.ENTITY_NOT_FOUND,
                                "User not found: ${post.userId}",
                            )
                    val recruitments =
                        (
                            recruitmentsByPostId[post.id]
                                ?: emptyList()
                        ).map { RecruitmentResponse.from(it) }
                    BookmarkedPostResponse.of(
                        post,
                        UserSimpleResponse.from(author),
                        recruitments,
                    )
                }
            }

            BookmarkType.TEAM -> {
                val teams = teamRepository.findByIdInOrderByIdDesc(targetIds)
                val teamIds = teams.map { it.id }
                val memberCountByTeamId =
                    memberRepository
                        .countByTeamIds(teamIds)
                        .associate { it.teamId to it.count.toInt() }
                val memberRoleByTeamId =
                    memberRepository
                        .findByUserIdAndTeamIdIn(user.id, teamIds)
                        .associate { it.teamId to it.role }
                teams.map {
                    TeamResponse.of(
                        it,
                        memberCountByTeamId[it.id] ?: 0,
                        memberRoleByTeamId[it.id],
                    )
                }
            }
        }
    }

    // 없는 ID를 북마크해 두면 나중에 그 ID로 생성된 대상이 목록에 나타남
    private fun checkBookmarkableTarget(
        type: BookmarkType,
        targetId: Long,
    ) {
        val exists =
            when (type) {
                BookmarkType.POST -> postRepository.existsById(targetId)
                BookmarkType.TEAM -> teamRepository.existsById(targetId)
            }
        if (!exists) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "$type not found: $targetId")
        }
    }
}
