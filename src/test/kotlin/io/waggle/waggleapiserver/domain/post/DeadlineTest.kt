package io.waggle.waggleapiserver.domain.post

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class DeadlineTest {
    @Test
    fun `마감일의 끝은 익일 KST 0시이므로 UTC 15시가 된다`() {
        val expiresAt = Deadline.toExpiresAt(LocalDate.of(2026, 9, 30))

        assertThat(expiresAt).isEqualTo(Instant.parse("2026-09-30T15:00:00Z"))
    }

    @Test
    fun `만료 시각을 되돌리면 원래 마감일이 나온다`() {
        val deadline = LocalDate.of(2026, 9, 30)

        assertThat(Deadline.toDeadline(Deadline.toExpiresAt(deadline))).isEqualTo(deadline)
    }

    @Test
    fun `마감일 당일 KST 오후는 아직 만료가 아니다`() {
        val expiresAt = Deadline.toExpiresAt(LocalDate.of(2026, 9, 30))

        // KST 2026-09-30 23:00
        assertThat(Instant.parse("2026-09-30T14:00:00Z")).isBefore(expiresAt)
    }

    @Test
    fun `마감일 익일 KST 0시부터 만료다`() {
        val expiresAt = Deadline.toExpiresAt(LocalDate.of(2026, 9, 30))

        assertThat(expiresAt).isEqualTo(Instant.parse("2026-09-30T15:00:00Z"))
        assertThat(Deadline.toDeadline(Instant.parse("2026-09-30T15:00:00Z")))
            .isEqualTo(LocalDate.of(2026, 9, 30))
    }
}
