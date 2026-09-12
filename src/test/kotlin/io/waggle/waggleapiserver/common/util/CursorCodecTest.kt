package io.waggle.waggleapiserver.common.util

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.post.repository.DeadlineGroup
import io.waggle.waggleapiserver.domain.post.repository.PostCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class CursorCodecTest {
    @Test
    fun `id 커서는 왕복한다`() {
        val cursor = IdCursor(123)

        assertThat(IdCursor.decode(cursor.encode())).isEqualTo(cursor)
    }

    @Test
    fun `복합 커서는 정렬 키까지 왕복한다`() {
        val viewCount = PostCursor.ViewCount(viewCount = 9, id = 123)
        val likeCount = PostCursor.LikeCount(likeCount = 2, id = 123)

        assertThat(PostCursor.decode(viewCount.encode(), PostSort.MOST_VIEWED)).isEqualTo(viewCount)
        assertThat(PostCursor.decode(likeCount.encode(), PostSort.MOST_LIKED)).isEqualTo(likeCount)
    }

    @Test
    fun `마감 커서는 마이크로초까지 보존한다`() {
        val cursor =
            PostCursor.Deadline(
                group = DeadlineGroup.EXPIRING,
                expiresAt = Instant.parse("2026-09-16T00:00:00.123456Z"),
                id = 123,
            )

        assertThat(PostCursor.decode(cursor.encode(), PostSort.DEADLINE_SOON)).isEqualTo(cursor)
    }

    @Test
    fun `무기한 모집 커서는 expiresAt이 없어도 왕복한다`() {
        val cursor = PostCursor.Deadline(DeadlineGroup.INDEFINITE, null, 123)

        assertThat(PostCursor.decode(cursor.encode(), PostSort.DEADLINE_SOON)).isEqualTo(cursor)
    }

    @Test
    fun `토큰은 항상 ey로 시작해 숫자 커서와 겹치지 않는다`() {
        val tokens =
            listOf(
                IdCursor(123).encode(),
                PostCursor.ViewCount(9, 123).encode(),
            )

        assertThat(tokens).allMatch { it.startsWith("ey") }
    }

    @Test
    fun `숫자 커서는 id 커서로 받아준다`() {
        assertThat(IdCursor.decode("123")).isEqualTo(IdCursor(123))
        assertThat(PostCursor.decode("123", PostSort.NEWEST)).isEqualTo(PostCursor.Id(123))
    }

    @Test
    fun `정렬이 다른 커서는 거부한다`() {
        val viewCount = PostCursor.ViewCount(9, 123).encode()

        assertThatThrownBy { PostCursor.decode(viewCount, PostSort.DEADLINE_SOON) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
    }

    @Test
    fun `모르는 필드가 든 커서는 무시하지 않고 거부한다`() {
        val deadline = PostCursor.Deadline(DeadlineGroup.EXPIRING, Instant.parse("2026-09-16T00:00:00Z"), 123).encode()

        assertThatThrownBy { PostCursor.decode(deadline, PostSort.NEWEST) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `가장 긴 커서도 요청 검증 한도 안에 든다`() {
        val longest =
            PostCursor.Deadline(
                group = DeadlineGroup.EXPIRING,
                expiresAt = Instant.parse("2026-09-16T23:59:59.999999Z"),
                id = Long.MAX_VALUE,
            )

        assertThat(longest.encode().length).isLessThanOrEqualTo(CursorCodec.MAX_LENGTH)
    }

    @Test
    fun `깨진 커서는 거부한다`() {
        assertThatThrownBy { IdCursor.decode("not-a-cursor") }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `마감 임박 묶음인데 expiresAt이 없으면 거부한다`() {
        val forged = PostCursor.Deadline(DeadlineGroup.EXPIRING, null, 123).encode()

        assertThatThrownBy { PostCursor.decode(forged, PostSort.DEADLINE_SOON) }
            .isInstanceOf(BusinessException::class.java)
    }
}
