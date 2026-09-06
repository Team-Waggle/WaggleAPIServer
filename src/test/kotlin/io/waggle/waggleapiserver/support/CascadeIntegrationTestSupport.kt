package io.waggle.waggleapiserver.support

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationRead
import io.waggle.waggleapiserver.domain.application.repository.ApplicationReadRepository
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.comment.Comment
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.comment.service.CommentService
import io.waggle.waggleapiserver.domain.follow.Follow
import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.like.Like
import io.waggle.waggleapiserver.domain.like.LikeId
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.like.service.LikeService
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.post.service.PostService
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.team.enums.WorkMode
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.team.service.TeamService
import io.waggle.waggleapiserver.domain.term.UserTermAgreement
import io.waggle.waggleapiserver.domain.term.repository.UserTermAgreementRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.domain.user.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

abstract class CascadeIntegrationTestSupport : IntegrationTestSupport() {
    // StorageClient(S3)와 AuthService(Redis)는 cascade 검증과 무관한 외부 부수효과라 목으로 대체
    @MockitoBean
    protected lateinit var storageClient: io.waggle.waggleapiserver.common.storage.StorageClient

    @MockitoBean
    protected lateinit var authService: AuthService

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    protected lateinit var teamService: TeamService

    @Autowired
    protected lateinit var postService: PostService

    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var teamRepository: TeamRepository

    @Autowired
    protected lateinit var memberRepository: MemberRepository

    @Autowired
    protected lateinit var postRepository: PostRepository

    @Autowired
    protected lateinit var commentService: CommentService

    @Autowired
    protected lateinit var commentRepository: CommentRepository

    @Autowired
    protected lateinit var recruitmentRepository: RecruitmentRepository

    @Autowired
    protected lateinit var applicationRepository: ApplicationRepository

    @Autowired
    protected lateinit var applicationReadRepository: ApplicationReadRepository

    @Autowired
    protected lateinit var bookmarkRepository: BookmarkRepository

    @Autowired
    protected lateinit var likeService: LikeService

    @Autowired
    protected lateinit var likeRepository: LikeRepository

    @Autowired
    protected lateinit var notificationRepository: NotificationRepository

    @Autowired
    protected lateinit var followRepository: FollowRepository

    @Autowired
    protected lateinit var userTermAgreementRepository: UserTermAgreementRepository

    @BeforeEach
    @AfterEach
    fun cleanDatabase() {
        val tables =
            listOf(
                "application_portfolio_urls",
                "application_reads",
                "applications",
                "recruitment_skills",
                "recruitments",
                "bookmarks",
                "likes",
                "comments",
                "notifications",
                "posts",
                "members",
                "follows",
                "user_term_agreements",
                "user_skills",
                "user_portfolios",
                "teams",
                "users",
            )
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        tables.forEach { jdbcTemplate.execute("DELETE FROM $it") }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    protected fun count(
        sql: String,
        vararg args: Any,
    ): Long = jdbcTemplate.queryForObject(sql, Long::class.java, *args)!!

    protected fun createUser(name: String): User =
        userRepository.save(
            User(
                provider = "google",
                providerId = name,
                email = "$name@test.com",
                profileImageUrl = null,
            ).apply {
                username = name
                position = Position.BACKEND
            },
        )

    protected fun createTeam(leaderId: UUID): Team =
        teamRepository.save(
            Team(
                name = "team-${UUID.randomUUID()}",
                description = "description",
                workMode = WorkMode.ONLINE,
                profileImageUrl = null,
                leaderId = leaderId,
                creatorId = leaderId,
            ),
        )

    protected fun createMember(
        userId: UUID,
        teamId: Long,
        role: MemberRole,
    ): Member =
        memberRepository.save(
            Member(
                userId = userId,
                teamId = teamId,
                position = Position.BACKEND,
                role = role,
            ),
        )

    protected fun createPost(
        userId: UUID,
        teamId: Long,
    ): Post =
        postRepository.save(
            Post(
                title = "title",
                content = "content",
                userId = userId,
                teamId = teamId,
            ),
        )

    protected fun createComment(
        postId: Long,
        userId: UUID,
        parentId: Long? = null,
        content: String = "content",
    ): Comment =
        commentRepository.save(
            Comment(
                postId = postId,
                userId = userId,
                parentId = parentId,
                content = content,
            ),
        )

    protected fun createRecruitment(postId: Long): Recruitment =
        recruitmentRepository.save(
            Recruitment(
                position = Position.BACKEND,
                count = 1,
                postId = postId,
            ),
        )

    protected fun createApplication(
        teamId: Long,
        postId: Long,
        userId: UUID,
    ): Application =
        applicationRepository.save(
            Application(
                position = Position.BACKEND,
                teamId = teamId,
                postId = postId,
                userId = userId,
                detail = null,
            ),
        )

    protected fun createApplicationRead(
        applicationId: Long,
        userId: UUID,
    ): ApplicationRead =
        applicationReadRepository.save(
            ApplicationRead(
                applicationId = applicationId,
                userId = userId,
            ),
        )

    protected fun createBookmark(
        userId: UUID,
        targetId: Long,
        type: BookmarkType,
    ): Bookmark = bookmarkRepository.save(Bookmark(BookmarkId(userId, targetId, type)))

    protected fun createLike(
        userId: UUID,
        type: LikeType,
        targetId: Long,
    ): Like = likeRepository.save(Like(LikeId(type, targetId, userId)))

    protected fun createNotification(
        userId: UUID,
        type: NotificationType,
        metadata: Map<String, Any?>,
    ): Notification =
        notificationRepository.save(
            Notification(
                type = type,
                userId = userId,
                metadata = metadata,
            ),
        )

    protected fun createFollow(
        followerId: UUID,
        followeeId: UUID,
    ): Follow =
        followRepository.save(
            Follow(
                followerId = followerId,
                followeeId = followeeId,
            ),
        )

    protected fun createUserTermAgreement(
        userId: UUID,
        termId: Long,
    ): UserTermAgreement =
        userTermAgreementRepository.save(
            UserTermAgreement(
                userId = userId,
                termId = termId,
            ),
        )
}
