package com.handys.assignment.repository

import com.handys.assignment.domain.CheckInCandidate
import com.handys.assignment.domain.Reservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ReservationRepository : JpaRepository<Reservation, Long> {
    @Query("""
        select new com.handys.assignment.domain.CheckInCandidate(
            r.id, r.roomNumber, r.scheduledCheckInAt, r.status, t.id, t.status
        )
        from Reservation r
        left join CleaningTask t on t.reservation = r
        where r.scheduledCheckInAt >= :start and r.scheduledCheckInAt < :end
    """)
    fun findCandidates(@Param("start") start: Instant, @Param("end") end: Instant): List<CheckInCandidate>
}
