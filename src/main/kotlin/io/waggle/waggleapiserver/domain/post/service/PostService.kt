package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpsertRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createPost(
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val (teamId, title, content, recruitments) = request

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, $teamId",
                )
        member.checkMemberRole(MemberRole.MEMBER)

        val post =
            Post(
                title = title,
                content = content,
                userId = user.id,
                teamId = teamId,
            )
        val savedPost = postRepository.save(post)

        val savedRecruitments =
            recruitmentRepository.saveAll(
                recruitments.map {
                    Recruitment(
                        position = it.position,
                        recruitingCount = it.recruitingCount,
                        postId = savedPost.id,
                    )
                },
            )

        return PostDetailResponse.of(
            savedPost,
            UserSimpleResponse.from(user),
            savedRecruitments.map { RecruitmentResponse.from(it) },
        )
    }

    fun getPosts(
        query: PostGetQuery,
        pageable: Pageable,
    ): Page<PostDetailResponse> {
        val posts = postRepository.findWithFilter(query.q, pageable)

        val userIds = posts.content.map { it.userId }.distinct()
        val userMap = userRepository.findAllById(userIds).associateBy { it.id }

        val postIds = posts.content.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

        return posts.map { post ->
            val user =
                userMap[post.userId]
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "User not found: ${post.userId}",
                    )
            val recruitments =
                (recruitmentsByPostId[post.id] ?: emptyList()).map { RecruitmentResponse.from(it) }
            PostDetailResponse.of(post, UserSimpleResponse.from(user), recruitments)
        }
    }

    fun getPost(postId: Long): PostDetailResponse {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        val user =
            userRepository.findByIdOrNull(post.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: $post.userId",
                )
        val recruitments =
            recruitmentRepository.findByPostId(postId).map { RecruitmentResponse.from(it) }
        return PostDetailResponse.of(post, UserSimpleResponse.from(user), recruitments)
    }

    @Transactional
    fun updatePost(
        postId: Long,
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val (teamId, title, content, recruitments) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        post.checkOwnership(user.id)
        post.update(title, content, teamId)

        val existingRecruitments = recruitmentRepository.findByPostId(postId)
        recruitmentRepository.deleteAll(existingRecruitments)

        val savedRecruitments =
            recruitmentRepository.saveAll(
                recruitments.map {
                    Recruitment(
                        position = it.position,
                        recruitingCount = it.recruitingCount,
                        postId = postId,
                    )
                },
            )

        return PostDetailResponse.of(
            post,
            UserSimpleResponse.from(user),
            savedRecruitments.map { RecruitmentResponse.from(it) },
        )
    }

    @Transactional
    fun closePostRecruitments(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, ${post.teamId}",
                )
        member.checkMemberRole(MemberRole.MANAGER)

        val recruitments = recruitmentRepository.findByPostId(postId)
        recruitments.forEach { it.close() }
    }

    @Transactional
    fun deletePost(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        post.checkOwnership(user.id)

        post.delete()
    }
}
