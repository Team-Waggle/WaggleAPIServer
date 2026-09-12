package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill

interface PostRepositoryCustom {
    fun findWithFilter(
        cursor: PostCursor?,
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        sort: PostSort,
        size: Int,
    ): List<PostWithCursor>
}

// 정렬 키를 아는 계층이 리포지토리뿐이라 다음 커서도 여기서 만들어 내보냄
data class PostWithCursor(
    val post: Post,
    val cursor: PostCursor,
)
