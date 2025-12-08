package io.waggle.waggleapiserver.domain.bookmark

interface Bookmarkable {
    val targetId: Long
    val type: BookmarkType
}
