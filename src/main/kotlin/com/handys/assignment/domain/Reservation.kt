package com.handys.assignment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

enum class ReservationStatus { RESERVED, CANCELLED, CHECKED_IN }

@Entity
@Table(name = "reservations", indexes = [Index(name = "idx_reservation_check_in", columnList = "scheduled_check_in_at")])
class Reservation(
    @Column(nullable = false)
    val roomNumber: String,
    @Column(name = "scheduled_check_in_at", nullable = false)
    val scheduledCheckInAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ReservationStatus = ReservationStatus.RESERVED,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
