package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PostServiceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var memberService: MemberService

    @Test
    fun `모집글 상세는 팀을 나가거나 추방된 사람만 이전 팀원으로 표시한다`() {
        val leader = createUser("leader")
        val member = createUser("member")
        val leaver = createUser("leaver")
        val removed = createUser("removed")
        val outsider = createUser("outsider")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(member.id, team.id, MemberRole.MEMBER)
        createMember(leaver.id, team.id, MemberRole.MEMBER)
        val removedMember = createMember(removed.id, team.id, MemberRole.MEMBER)
        val post = createPost(leader.id, team.id)
        memberService.leaveTeam(team.id, leaver)
        memberService.removeMember(removedMember.id, leader)

        assertThat(postService.getPost(post.id, leaver).formerMember).isTrue()
        assertThat(postService.getPost(post.id, removed).formerMember).isTrue()
        assertThat(postService.getPost(post.id, member).formerMember).isFalse()
        assertThat(postService.getPost(post.id, outsider).formerMember).isFalse()
        assertThat(postService.getPost(post.id, null).formerMember).isFalse()
    }

    @Test
    fun `팀을 떠난 작성자는 자기 모집글을 수정하거나 삭제할 수 없다`() {
        val leader = createUser("leader")
        val author = createUser("author")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val authorMember = createMember(author.id, team.id, MemberRole.MANAGER)
        val post = createPostWithRecruitment(author, team.id)
        memberService.removeMember(authorMember.id, leader)

        assertAccessDenied { postService.updatePost(post.id, updateRequest("수정"), author) }
        assertAccessDenied { postService.deletePost(post.id, author) }
        assertThat(postRepository.findById(post.id).get().title).isEqualTo("title")
    }

    @Test
    fun `팀장과 관리자는 남이 쓴 모집글을 수정하고 삭제할 수 있다`() {
        val leader = createUser("leader")
        val manager = createUser("manager")
        val author = createUser("author")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(manager.id, team.id, MemberRole.MANAGER)
        createMember(author.id, team.id, MemberRole.MEMBER)
        val post = createPostWithRecruitment(author, team.id)

        postService.updatePost(post.id, updateRequest("관리자 수정"), manager)
        assertThat(postRepository.findById(post.id).get().title).isEqualTo("관리자 수정")

        postService.deletePost(post.id, leader)
        assertThat(postRepository.findById(post.id)).isEmpty()
    }

    @Test
    fun `일반 팀원은 남이 쓴 모집글을 수정하거나 삭제할 수 없고 자기 글은 할 수 있다`() {
        val leader = createUser("leader")
        val member = createUser("member")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        createMember(member.id, team.id, MemberRole.MEMBER)
        val leaderPost = createPostWithRecruitment(leader, team.id)
        val memberPost = createPostWithRecruitment(member, team.id)

        assertAccessDenied { postService.updatePost(leaderPost.id, updateRequest("수정"), member) }
        assertAccessDenied { postService.deletePost(leaderPost.id, member) }

        postService.updatePost(memberPost.id, updateRequest("본인 수정"), member)
        assertThat(postRepository.findById(memberPost.id).get().title).isEqualTo("본인 수정")
    }

    private fun createPostWithRecruitment(
        author: User,
        teamId: Long,
    ): Post = createPost(author.id, teamId).also { createRecruitment(it.id) }

    private fun updateRequest(title: String) =
        PostUpdateRequest(
            title = title,
            content = "content",
            recruitments =
                listOf(
                    RecruitmentUpsertRequest(position = Position.BACKEND, count = 1, skills = emptySet()),
                ),
            deadline = null,
        )

    private fun assertAccessDenied(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACCESS_DENIED)
    }
}
