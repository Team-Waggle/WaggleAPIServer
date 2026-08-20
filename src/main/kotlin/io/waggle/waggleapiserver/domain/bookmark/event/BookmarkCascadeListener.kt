package io.waggle.waggleapiserver.domain.bookmark.event

import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class BookmarkCascadeListener(
    private val bookmarkRepository: BookmarkRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        bookmarkRepository.deleteByPostTeamId(event.teamId)
        bookmarkRepository.deleteByIdTargetIdAndIdType(event.teamId, BookmarkType.TEAM)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        bookmarkRepository.deleteByIdTargetIdAndIdType(event.postId, BookmarkType.POST)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        bookmarkRepository.deleteByPostUserId(event.userId)
        bookmarkRepository.deleteByIdUserId(event.userId)
    }
}
