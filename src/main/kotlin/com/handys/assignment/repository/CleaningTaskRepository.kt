package com.handys.assignment.repository

import com.handys.assignment.domain.CleaningStatus
import com.handys.assignment.domain.CleaningTask
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface CleaningTaskRepository : JpaRepository<CleaningTask, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CleaningTask t set t.status = :completed, t.completedAt = :completedAt
        where t.id = :id and t.status = :pending
    """)
    fun completeIfPending(
        @Param("id") id: Long,
        @Param("completedAt") completedAt: Instant,
        @Param("completed") completed: CleaningStatus,
        @Param("pending") pending: CleaningStatus,
    ): Int
}
