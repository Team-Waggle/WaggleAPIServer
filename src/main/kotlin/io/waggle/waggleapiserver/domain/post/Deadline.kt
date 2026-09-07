package io.waggle.waggleapiserver.domain.post

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 클라이언트는 마감일(날짜)을 주고받고 서버는 만료 시각(인스턴트)으로 저장한다.
 * 배타적 경계라 마감일의 끝 = 익일 KST 0시이며, 이 변환이 두 계층에 흩어지면
 * 화면의 마감일과 실제 차단 시점이 어긋남
 */
object Deadline {
    private val KST = ZoneId.of("Asia/Seoul")

    fun toExpiresAt(deadline: LocalDate): Instant = deadline.plusDays(1).atStartOfDay(KST).toInstant()

    fun toDeadline(expiresAt: Instant): LocalDate = expiresAt.atZone(KST).toLocalDate().minusDays(1)

    fun todayExpiresAt(): Instant = toExpiresAt(LocalDate.now(KST))

    // 오늘을 마감일로 고르는 것은 정당하므로 오늘의 경계까지는 과거가 아님
    fun isPast(expiresAt: Instant): Boolean = expiresAt < todayExpiresAt()
}
