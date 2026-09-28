package com.handys.assignment.service

import com.handys.assignment.domain.CheckInReadinessPolicy
import com.handys.assignment.domain.ReadinessReport
import com.handys.assignment.repository.ReservationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class ReadinessService(
    private val reservations: ReservationRepository,
    private val policy: CheckInReadinessPolicy,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun findRisks(): ReadinessReport {
        val now = clock.instant()
        val today = now.atZone(CheckInReadinessPolicy.SITE_ZONE).toLocalDate()
        val start = today.atStartOfDay(CheckInReadinessPolicy.SITE_ZONE).toInstant()
        val end = today.plusDays(1).atStartOfDay(CheckInReadinessPolicy.SITE_ZONE).toInstant()
        return ReadinessReport(now, policy.assess(reservations.findCandidates(start, end), now))
    }
}
