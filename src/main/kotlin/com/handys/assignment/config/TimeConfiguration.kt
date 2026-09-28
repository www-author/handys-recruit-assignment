package com.handys.assignment.config

import com.handys.assignment.domain.CheckInReadinessPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Configuration
class TimeConfiguration {
    @Bean
    fun clock(@Value("\${app.clock.fixed-at:}") fixedAt: String): Clock =
        if (fixedAt.isBlank()) Clock.systemUTC() else Clock.fixed(Instant.parse(fixedAt), ZoneOffset.UTC)

    @Bean
    fun readinessPolicy(): CheckInReadinessPolicy = CheckInReadinessPolicy()
}
