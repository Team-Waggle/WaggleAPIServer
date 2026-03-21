package io.waggle.waggleapiserver.domain.notification

enum class NotificationType {
    APPLICATION_RECEIVED,
    APPLICATION_ACCEPTED,
    APPLICATION_REJECTED,
    APPLICATION_REMIND,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEMBER_REMOVED,
    REVIEW_REQUESTED,
    REVIEW_RECEIVED,
}
