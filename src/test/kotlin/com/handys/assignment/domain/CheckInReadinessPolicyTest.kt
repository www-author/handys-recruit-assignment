package com.handys.assignment.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInReadinessPolicyTest {
    private val policy = CheckInReadinessPolicy()
    private val now = Clock.fixed(Instant.parse("2026-09-28T05:30:00Z"), ZoneOffset.UTC).instant()

    private fun candidate(
        id: Long = 1,
        at: Instant = Instant.parse("2026-09-28T06:00:00Z"),
        status: ReservationStatus = ReservationStatus.RESERVED,
        cleaning: CleaningStatus? = CleaningStatus.PENDING,
    ) = CheckInCandidate(id, "301", at, status, if (cleaning == null) null else id, cleaning)

    @Test
    fun `AC-01 정확히 60분 후는 포함하고 1초 초과하면 제외한다`() {
        val result = policy.assess(listOf(
            candidate(1, Instant.parse("2026-09-28T06:30:00Z")),
            candidate(2, Instant.parse("2026-09-28T06:30:01Z")),
        ), now)
        assertEquals(listOf(1L), result.map { it.reservationId })
    }

    @Test
    fun `AC-02 오늘 시작은 포함하고 어제와 내일은 제외한다`() {
        val result = policy.assess(listOf(
            candidate(1, Instant.parse("2026-09-27T14:59:59Z")),
            candidate(2, Instant.parse("2026-09-27T15:00:00Z")),
            candidate(3, Instant.parse("2026-09-28T15:00:00Z")),
        ), now)
        assertEquals(listOf(2L), result.map { it.reservationId })
    }

    @Test
    fun `AC-02 60분 이내라도 서울 자정을 넘는 예약은 제외한다`() {
        val lateNight = Instant.parse("2026-09-28T14:30:00Z")
        val result = policy.assess(listOf(
            candidate(1, Instant.parse("2026-09-28T14:59:59Z")),
            candidate(2, Instant.parse("2026-09-28T15:00:00Z")),
        ), lateNight)
        assertEquals(listOf(1L), result.map { it.reservationId })
    }

    @Test
    fun `AC-02 서울 자정 직후에는 UTC 날짜가 아니라 서울 날짜를 사용한다`() {
        val midnight = Instant.parse("2026-09-27T15:00:00Z")
        val result = policy.assess(listOf(
            candidate(1, Instant.parse("2026-09-27T14:59:59Z")),
            candidate(2, midnight),
        ), midnight)
        assertEquals(listOf(2L), result.map { it.reservationId })
        assertFalse(result.single().overdue)
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus::class, names = ["CANCELLED", "CHECKED_IN"])
    fun `AC-03 취소 또는 체크인한 예약은 제외한다`(status: ReservationStatus) {
        assertTrue(policy.assess(listOf(candidate(status = status)), now).isEmpty())
    }

    @Test
    fun `AC-03 청소 완료 예약은 제외한다`() {
        assertTrue(policy.assess(listOf(candidate(cleaning = CleaningStatus.COMPLETED)), now).isEmpty())
    }

    @Test
    fun `AC-04 미완료와 작업 없음의 사유를 구분한다`() {
        val result = policy.assess(listOf(candidate(1), candidate(2, cleaning = null)), now)
        assertEquals(listOf(RiskReason.CLEANING_PENDING, RiskReason.CLEANING_STATUS_UNKNOWN), result.map { it.reason })
        assertEquals(null, result[1].cleaningTaskId)
    }

    @Test
    fun `AC-05 예정 시각이 지난 경우만 경과 상태다`() {
        val result = policy.assess(listOf(
            candidate(1, Instant.parse("2026-09-28T05:29:59Z")),
            candidate(2, now),
            candidate(3, Instant.parse("2026-09-28T05:30:01Z")),
        ), now)
        assertEquals(listOf(true, false, false), result.map { it.overdue })
    }

    @Test
    fun `AC-05 예정 시각 다음 예약 ID 순으로 정렬한다`() {
        val result = policy.assess(listOf(candidate(3), candidate(2, now), candidate(1)), now)
        assertEquals(listOf(2L, 1L, 3L), result.map { it.reservationId })
    }

    @Test
    fun `AC-05 후보가 없으면 빈 목록을 반환한다`() {
        assertTrue(policy.assess(emptyList(), now).isEmpty())
    }
}
