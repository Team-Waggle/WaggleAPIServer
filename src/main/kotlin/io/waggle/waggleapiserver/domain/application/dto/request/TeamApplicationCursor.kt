package io.waggle.waggleapiserver.domain.application.dto.request

import io.waggle.waggleapiserver.common.util.Cursor
import io.waggle.waggleapiserver.common.util.CursorCodec

data class TeamApplicationCursor(
    val statusPriority: Int,
    val id: Long,
) : Cursor {
    companion object {
        fun decode(cursor: String): TeamApplicationCursor =
            CursorCodec.decode(cursor, TeamApplicationCursor::class.java)
    }
}
