package com.handys.assignment.api

import com.handys.assignment.domain.CheckInReadinessPolicy
import com.handys.assignment.domain.CleaningStatus
import com.handys.assignment.domain.RiskReason
import com.handys.assignment.service.CleaningService
import com.handys.assignment.service.ReadinessService
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.OffsetDateTime

data class RiskItemResponse(
    val reservationId: Long,
    val roomNumber: String,
    val scheduledCheckInAt: OffsetDateTime,
    val cleaningTaskId: Long?,
    val reason: RiskReason,
    val overdue: Boolean,
)

data class RisksResponse(val evaluatedAt: OffsetDateTime, val items: List<RiskItemResponse>)
data class CleaningResponse(val id: Long, val status: CleaningStatus, val completedAt: OffsetDateTime)

private fun Instant.inSeoul(): OffsetDateTime = atZone(CheckInReadinessPolicy.SITE_ZONE).toOffsetDateTime()

@RestController
class OperationsController(private val readiness: ReadinessService, private val cleaning: CleaningService) {
    @GetMapping("/ops/check-in-risks")
    fun findRisks(): RisksResponse {
        val report = readiness.findRisks()
        return RisksResponse(report.evaluatedAt.inSeoul(), report.items.map {
            RiskItemResponse(it.reservationId, it.roomNumber, it.scheduledCheckInAt.inSeoul(), it.cleaningTaskId, it.reason, it.overdue)
        })
    }

    @PatchMapping("/cleaning-tasks/{id}/complete")
    fun complete(@PathVariable @Positive id: Long): CleaningResponse {
        val result = cleaning.complete(id)
        return CleaningResponse(result.id, result.status, result.completedAt.inSeoul())
    }
}
