package com.handys.assignment.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

enum class CleaningStatus { PENDING, COMPLETED }

@Entity
@Table(
    name = "cleaning_tasks",
    check = [CheckConstraint(
        name = "chk_cleaning_completion",
        constraint = "(status = 'PENDING' AND completed_at IS NULL) OR (status = 'COMPLETED' AND completed_at IS NOT NULL)",
    )],
)
class CleaningTask(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    val reservation: Reservation,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: CleaningStatus = CleaningStatus.PENDING,
    @Column(name = "completed_at")
    val completedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
