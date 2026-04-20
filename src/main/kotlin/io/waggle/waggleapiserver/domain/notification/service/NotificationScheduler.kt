package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.repository.ApplicationReadRepository
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Component
class NotificationScheduler(
    private val applicationRepository: ApplicationRepository,
    private val applicationReadRepository: ApplicationReadRepository,
    private val memberRepository: MemberRepository,
    private val notificationRepository: NotificationRepository,
    private val recruitmentRepository: RecruitmentRepository,
) {
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun remindUnreadApplications() {
        val now = Instant.now()
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)
        val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)

        val pendingApplications =
            applicationRepository.findByStatusAndCreatedAtBetween(
                ApplicationStatus.PENDING,
                threeDaysAgo,
                twoDaysAgo,
            )

        if (pendingApplications.isEmpty()) return

        val postIds = pendingApplications.map { it.postId }.distinct()
        val recruitingPostIdSet =
            recruitmentRepository
                .findByPostIdIn(postIds)
                .filter { it.status == RecruitmentStatus.RECRUITING }
                .map { it.postId }
                .toSet()

        val targetApplications = pendingApplications.filter { it.postId in recruitingPostIdSet }

        if (targetApplications.isEmpty()) return

        val applicationIds = targetApplications.map { it.id }
        val teamIds = targetApplications.map { it.teamId }.distinct()

        val membersByTeamId =
            teamIds.associateWith { teamId ->
                memberRepository.findByTeamIdAndRoleIn(
                    teamId,
                    listOf(MemberRole.MANAGER, MemberRole.LEADER),
                )
            }

        val memberUserIds =
            membersByTeamId.values
                .flatten()
                .map { it.userId }
                .distinct()

        val userIdToReadApplicationIdSet =
            applicationReadRepository
                .findByApplicationIdInAndUserIdIn(applicationIds, memberUserIds)
                .map { it.userId to it.applicationId }
                .toSet()

        val userIdToRemindedTeamIdSet =
            notificationRepository
                .findByUserIdInAndTypeAndCreatedAtAfter(
                    memberUserIds,
                    NotificationType.APPLICATION_REMIND,
                    threeDaysAgo,
                ).map { notification ->
                    notification.userId to (notification.metadata["teamId"] as? Number)?.toLong()
                }.toSet()

        val unreadCountByUserIdToTeamId = mutableMapOf<Pair<UUID, Long>, Int>()

        for (application in targetApplications) {
            val members = membersByTeamId[application.teamId] ?: continue
            for (member in members) {
                if ((member.userId to application.id) in userIdToReadApplicationIdSet) continue
                val userIdToTeamId = member.userId to application.teamId
                unreadCountByUserIdToTeamId[userIdToTeamId] =
                    (unreadCountByUserIdToTeamId[userIdToTeamId] ?: 0) + 1
            }
        }

        val notifications =
            unreadCountByUserIdToTeamId
                .filter { (userIdToTeamId, _) -> userIdToTeamId !in userIdToRemindedTeamIdSet }
                .map { (userIdToTeamId, count) ->
                    Notification(
                        type = NotificationType.APPLICATION_REMIND,
                        userId = userIdToTeamId.first,
                        metadata =
                            mapOf(
                                "teamId" to userIdToTeamId.second,
                                "unreadApplicationCount" to count,
                            ),
                    )
                }

        notificationRepository.saveAll(notifications)
    }
}
