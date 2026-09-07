package io.waggle.waggleapiserver.domain.recruitment.service

import io.waggle.waggleapiserver.common.util.logger
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 마감일의 끝은 KST 0시이므로 그 시각(UTC 15시)에 돌린다.
 * 조건이 expires_at <= now라 놓친 날이 있어도 다음 실행에서 함께 정리됨
 */
@Component
class RecruitmentScheduler(
    private val recruitmentRepository: RecruitmentRepository,
) {
    @Scheduled(cron = "0 0 15 * * *", zone = "UTC")
    @Transactional
    fun closeExpiredRecruitments() {
        val closed = recruitmentRepository.updateStatusToClosedByPostExpired()
        if (closed > 0) {
            logger.info("Closed $closed recruitments of expired posts")
        }
    }
}
