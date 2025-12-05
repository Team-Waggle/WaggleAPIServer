package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.dto.request.NotificationCreateRequest
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun createNotification(request: NotificationCreateRequest) {
        val (type, projectId, userId) = request

        if (!userRepository.existsById(userId)) {
            throw EntityNotFoundException("User not found: $userId")
        }

        if (projectId != null && !projectRepository.existsById(projectId)) {
            throw EntityNotFoundException("Project not found: $projectId")
        }

        val notification =
            Notification(
                type = type,
                projectId = projectId,
                userId = userId,
            )

        notificationRepository.save(notification)
    }

    fun getUserNotifications(user: User): List<NotificationResponse> {
        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.id)

        val projectIds = notifications.mapNotNull { it.projectId }
        val projectMap = projectRepository.findAllById(projectIds).associateBy { it.id }

        return notifications.map { notification ->
            val project =
                notification.projectId?.let { projectMap[it] }?.let(ProjectSimpleResponse::from)

            NotificationResponse.of(
                notification = notification,
                project = project,
            )
        }
    }
}
