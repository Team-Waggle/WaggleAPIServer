package io.waggle.waggleapiserver.domain.recruitment

import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.recruitment.service.RecruitmentScheduler
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class RecruitmentSchedulerTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var recruitmentScheduler: RecruitmentScheduler

    @Test
    fun `스케줄러는 기한이 지난 모집글의 포지션만 닫는다`() {
        val expired = createPostWithDeadline(Instant.now().minus(1, ChronoUnit.DAYS))
        val alive = createPostWithDeadline(Instant.now().plus(1, ChronoUnit.DAYS))
        val indefinite = createPostWithDeadline(null)
        listOf(expired, alive, indefinite).forEach { createRecruitment(it.id) }

        recruitmentScheduler.closeExpiredRecruitments()

        assertThat(recruitmentRepository.findByPostId(expired.id))
            .allMatch { it.status == RecruitmentStatus.CLOSED }
        assertThat(recruitmentRepository.findByPostId(alive.id))
            .allMatch { it.status == RecruitmentStatus.RECRUITING }
        assertThat(recruitmentRepository.findByPostId(indefinite.id))
            .allMatch { it.status == RecruitmentStatus.RECRUITING }
    }

    private fun createPostWithDeadline(expiresAt: Instant?): Post {
        val author = createUser("author-${System.nanoTime()}")
        val team = createTeam(author.id)
        return postRepository.save(
            Post(
                title = "title",
                content = "content",
                userId = author.id,
                teamId = team.id,
                expiresAt = expiresAt,
            ),
        )
    }
}
