package com.handys.assignment.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class CheckInCandidate(
    val reservationId: Long,
    val roomNumber: String,
    val scheduledCheckInAt: Instant,
    val reservationStatus: ReservationStatus,
    val cleaningTaskId: Long?,
    val cleaningStatus: CleaningStatus?,
)

enum class RiskReason { CLEANING_PENDING, CLEANING_STATUS_UNKNOWN }

data class CheckInRisk(
    val reservationId: Long,
    val roomNumber: String,
    val scheduledCheckInAt: Instant,
    val cleaningTaskId: Long?,
    val reason: RiskReason,
    val overdue: Boolean,
)

data class ReadinessReport(val evaluatedAt: Instant, val items: List<CheckInRisk>)

class CheckInReadinessPolicy {
    fun assess(candidates: List<CheckInCandidate>, now: Instant): List<CheckInRisk> {
        val today = now.atZone(SITE_ZONE).toLocalDate()
        val threshold = now.plus(Duration.ofMinutes(60))
        return candidates.asSequence()
            .filter { it.reservationStatus == ReservationStatus.RESERVED }
            .filter { it.scheduledCheckInAt.atZone(SITE_ZONE).toLocalDate() == today }
            .filter { !it.scheduledCheckInAt.isAfter(threshold) }
            .filter { it.cleaningStatus != CleaningStatus.COMPLETED }
            .map {
                CheckInRisk(
                    reservationId = it.reservationId,
                    roomNumber = it.roomNumber,
                    scheduledCheckInAt = it.scheduledCheckInAt,
                    cleaningTaskId = it.cleaningTaskId,
                    reason = if (it.cleaningStatus == null) RiskReason.CLEANING_STATUS_UNKNOWN else RiskReason.CLEANING_PENDING,
                    overdue = it.scheduledCheckInAt.isBefore(now),
                )
            }
            .sortedWith(compareBy(CheckInRisk::scheduledCheckInAt, CheckInRisk::reservationId))
            .toList()
    }

    companion object {
        val SITE_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
