package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
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
}
