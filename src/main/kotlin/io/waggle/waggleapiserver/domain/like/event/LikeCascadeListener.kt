package io.waggle.waggleapiserver.domain.like.event

import io.waggle.waggleapiserver.domain.comment.event.CommentDeletedEvent
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// likes 행을 지우는 쿼리마다 like_count 갱신을 짝지어 둠
// 삭제되거나 tombstone된 행도 like_count가 실제 likes 수와 같아야 함
@Component
class LikeCascadeListener(
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onCommentDeleted(event: CommentDeletedEvent) {
        likeRepository.deleteByIdTypeAndIdTargetId(LikeType.COMMENT, event.commentId)
        commentRepository.resetLikeCount(event.commentId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        likeRepository.deleteByIdTypeAndIdTargetId(LikeType.POST, event.postId)
        postRepository.resetLikeCount(event.postId)
        likeRepository.deleteByCommentPostId(event.postId)
        commentRepository.resetLikeCountByPostId(event.postId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        likeRepository.deleteByPostTeamId(event.teamId)
        postRepository.resetLikeCountByTeamId(event.teamId)
        likeRepository.deleteByCommentPostTeamId(event.teamId)
        commentRepository.resetLikeCountByPostInTeamId(event.teamId)
    }

    // 그 사용자가 누른 좋아요와 그 사용자의 글/댓글이 받은 좋아요를 모두 정리
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        postRepository.decreaseLikeCountByLikerId(event.userId)
        commentRepository.decreaseLikeCountByLikerId(event.userId)
        likeRepository.deleteByIdUserId(event.userId)
        likeRepository.deleteByPostUserId(event.userId)
        postRepository.resetLikeCountByUserId(event.userId)
        likeRepository.deleteByCommentUserId(event.userId)
        commentRepository.resetLikeCountByUserId(event.userId)
    }
}
