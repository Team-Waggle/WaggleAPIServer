package io.waggle.waggleapiserver.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import java.util.Base64

interface Cursor {
    fun encode(): String = CursorCodec.encode(this)
}

object CursorCodec {
    const val MAX_LENGTH = 256

    // Spring 빈 매퍼는 FAIL_ON_UNKNOWN_PROPERTIES가 꺼져 있어 다른 정렬의 토큰을 조용히 통과시킴
    private val OBJECT_MAPPER =
        ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun encode(cursor: Cursor): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(OBJECT_MAPPER.writeValueAsBytes(cursor))

    fun <T : Cursor> decode(
        cursor: String,
        type: Class<T>,
    ): T =
        runCatching { OBJECT_MAPPER.readValue(Base64.getUrlDecoder().decode(cursor), type) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Invalid cursor") }

    // 배포 시점 클라이언트의 숫자 커서 호환용, 프론트 배포 후 제거 대상
    fun decodeLegacyId(cursor: String): Long? = cursor.toLongOrNull()
}

data class IdCursor(
    val id: Long,
) : Cursor {
    companion object {
        fun decode(cursor: String): IdCursor =
            CursorCodec.decodeLegacyId(cursor)?.let { IdCursor(it) }
                ?: CursorCodec.decode(cursor, IdCursor::class.java)
    }
}
