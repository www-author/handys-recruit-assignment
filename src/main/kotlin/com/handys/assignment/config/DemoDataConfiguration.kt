package com.handys.assignment.config

import com.handys.assignment.domain.CleaningStatus
import com.handys.assignment.domain.CleaningTask
import com.handys.assignment.domain.Reservation
import com.handys.assignment.domain.ReservationStatus
import com.handys.assignment.repository.CleaningTaskRepository
import com.handys.assignment.repository.ReservationRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock

@Configuration
@Profile("demo")
class DemoDataConfiguration {
    @Bean
    fun demoData(reservations: ReservationRepository, tasks: CleaningTaskRepository, clock: Clock, transaction: TransactionTemplate) = ApplicationRunner {
        transaction.executeWithoutResult {
            val now = clock.instant()
            fun seed(room: String, minutes: Long, status: ReservationStatus = ReservationStatus.RESERVED, cleaning: CleaningStatus? = CleaningStatus.PENDING) {
                val reservation = reservations.save(Reservation(room, now.plusSeconds(minutes * 60), status))
                if (cleaning != null) {
                    tasks.save(CleaningTask(reservation, cleaning, if (cleaning == CleaningStatus.COMPLETED) now.minusSeconds(3600) else null))
                }
            }
            seed("301", 30)
            seed("302", 30, cleaning = CleaningStatus.COMPLETED)
            seed("303", 150)
            seed("304", -30)
            seed("305", 30, cleaning = null)
            seed("306", 30, status = ReservationStatus.CANCELLED)
            seed("307", 30, status = ReservationStatus.CHECKED_IN)
            seed("308", 1440)
        }
    }
}
