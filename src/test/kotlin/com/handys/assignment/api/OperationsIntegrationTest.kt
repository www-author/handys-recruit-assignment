package com.handys.assignment.api

import com.handys.assignment.domain.CleaningStatus
import com.handys.assignment.domain.CleaningTask
import com.handys.assignment.domain.Reservation
import com.handys.assignment.domain.ReservationStatus
import com.handys.assignment.repository.CleaningTaskRepository
import com.handys.assignment.repository.ReservationRepository
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@Import(OperationsIntegrationTest.ClockConfig::class)
class OperationsIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val reservations: ReservationRepository,
    private val tasks: CleaningTaskRepository,
    private val clock: ControlledClock,
    private val mapper: ObjectMapper,
) {
    @TestConfiguration(proxyBeanMethods = false)
    class ClockConfig {
        @Bean
        @Primary
        fun controlledClock() = ControlledClock()
    }

    @BeforeEach
    fun reset() {
        clock.reset()
        tasks.deleteAllInBatch()
        reservations.deleteAllInBatch()
    }

    private fun reserve(
        room: String = "301",
        at: String = "2026-09-28T06:00:00Z",
        status: ReservationStatus = ReservationStatus.RESERVED,
    ): Reservation = reservations.saveAndFlush(Reservation(room, Instant.parse(at), status))

    private fun pending(reservation: Reservation = reserve()): CleaningTask = tasks.saveAndFlush(CleaningTask(reservation))

    private fun complete(id: Long): String = mvc.perform(patch("/cleaning-tasks/{id}/complete", id))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andReturn().response.contentAsString

    @Test
    fun `AC-01부터 AC-05 실제 저장 데이터의 필터 정렬 사유와 시각을 반환한다`() {
        val upcoming = reserve("301")
        val upcomingTask = pending(upcoming)
        reserve("305")
        pending(reserve("304", "2026-09-28T05:00:00Z"))
        pending(reserve("306", "2026-09-28T06:30:00Z"))
        pending(reserve("307", "2026-09-28T06:30:01Z"))
        pending(reserve("308", status = ReservationStatus.CANCELLED))
        pending(reserve("309", status = ReservationStatus.CHECKED_IN))
        tasks.saveAndFlush(CleaningTask(reserve("310"), CleaningStatus.COMPLETED, Instant.parse("2026-09-28T04:00:00Z")))
        pending(reserve("311", "2026-09-27T14:59:59Z"))
        pending(reserve("312", "2026-09-28T15:00:00Z"))

        val body = mvc.perform(get("/ops/check-in-risks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.evaluatedAt").value("2026-09-28T14:30:00+09:00"))
            .andExpect(jsonPath("$.items", hasSize<Any>(4)))
            .andExpect(jsonPath("$.items[0].roomNumber").value("304"))
            .andExpect(jsonPath("$.items[0].overdue").value(true))
            .andExpect(jsonPath("$.items[1].reservationId").value(upcoming.id))
            .andExpect(jsonPath("$.items[1].cleaningTaskId").value(upcomingTask.id))
            .andExpect(jsonPath("$.items[1].scheduledCheckInAt").value("2026-09-28T15:00:00+09:00"))
            .andExpect(jsonPath("$.items[1].reason").value("CLEANING_PENDING"))
            .andExpect(jsonPath("$.items[1].overdue").value(false))
            .andExpect(jsonPath("$.items[2].roomNumber").value("305"))
            .andExpect(jsonPath("$.items[2].reason").value("CLEANING_STATUS_UNKNOWN"))
            .andExpect(jsonPath("$.items[3].roomNumber").value("306"))
            .andReturn().response.contentAsString
        val unknown = mapper.readTree(body)["items"][2]
        assertTrue(unknown.has("cleaningTaskId"))
        assertTrue(unknown["cleaningTaskId"].isNull)
        assertEquals(1, clock.reads.get())
    }

    @Test
    fun `AC-02 조회 쿼리는 서울 날짜의 시작을 포함하고 다음 날 시작은 제외한다`() {
        clock.current.set(Instant.parse("2026-09-28T14:30:00Z"))
        pending(reserve("yesterday", "2026-09-27T14:59:59Z"))
        pending(reserve("today-start", "2026-09-27T15:00:00Z"))
        pending(reserve("today-end", "2026-09-28T14:59:59Z"))
        pending(reserve("tomorrow", "2026-09-28T15:00:00Z"))
        mvc.perform(get("/ops/check-in-risks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(2)))
            .andExpect(jsonPath("$.items[0].roomNumber").value("today-start"))
            .andExpect(jsonPath("$.items[1].roomNumber").value("today-end"))
    }

    @Test
    fun `AC-05 빈 결과도 200과 빈 배열을 반환한다`() {
        mvc.perform(get("/ops/check-in-risks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(0)))
    }

    @Test
    fun `AC-06과 AC-07 청소 완료 후 목록에서 제외되고 반복 요청은 최초 시각을 유지한다`() {
        val task = pending()
        val id = requireNotNull(task.id)
        mvc.perform(get("/ops/check-in-risks"))
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
        val first = complete(id)
        clock.current.set(Instant.parse("2026-09-28T05:45:00Z"))
        assertEquals(first, complete(id))
        val saved = tasks.findById(id).orElseThrow()
        assertEquals(CleaningStatus.COMPLETED, saved.status)
        assertEquals(Instant.parse("2026-09-28T05:30:00Z"), saved.completedAt)
        mvc.perform(get("/ops/check-in-risks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(0)))
    }

    @Test
    fun `AC-07 서로 다른 시각의 동시 완료 요청도 같은 최초 완료 결과를 반환한다`() {
        val id = requireNotNull(pending().id)
        clock.overlapNextTwoReads()
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map { executor.submit(Callable { complete(id) }) }
            val responses = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertEquals(responses[0], responses[1])
            val completedAt = OffsetDateTime.parse(mapper.readTree(responses[0])["completedAt"].stringValue()).toInstant()
            assertTrue(completedAt in setOf(Instant.parse("2026-09-28T05:30:00Z"), Instant.parse("2026-09-28T05:30:01Z")))
            assertEquals(completedAt, tasks.findById(id).orElseThrow().completedAt)
            assertEquals(CleaningStatus.COMPLETED, tasks.findById(id).orElseThrow().status)
            assertEquals(2, clock.reads.get())
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc", "0", "-1", "1.5", "9223372036854775808"])
    fun `AC-08 잘못된 ID는 400을 반환한다`(id: String) {
        mvc.perform(patch("/cleaning-tasks/{id}/complete", id))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_TASK_ID"))
        assertEquals(0L, tasks.count())
    }

    @Test
    fun `AC-08 없는 작업은 404이고 누락된 작업을 생성하지 않는다`() {
        reserve()
        mvc.perform(patch("/cleaning-tasks/9223372036854775807/complete"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CLEANING_TASK_NOT_FOUND"))
        assertEquals(0L, tasks.count())
        mvc.perform(get("/ops/check-in-risks"))
            .andExpect(jsonPath("$.items[0].reason").value("CLEANING_STATUS_UNKNOWN"))
    }

    @Test
    fun `같은 객실의 이전 투숙 청소 완료를 이번 예약에 재사용하지 않는다`() {
        val old = reserve("301", "2026-09-27T06:00:00Z")
        tasks.saveAndFlush(CleaningTask(old, CleaningStatus.COMPLETED, Instant.parse("2026-09-27T05:00:00Z")))
        val current = reserve("301")
        pending(current)
        mvc.perform(get("/ops/check-in-risks"))
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].reservationId").value(current.id))
    }

    @Test
    fun `예약당 청소 작업은 DB 유일 제약으로 하나만 허용한다`() {
        val reservation = reserve()
        pending(reservation)
        assertFailsWith<DataIntegrityViolationException> { pending(reservation) }
        assertEquals(1L, tasks.count())
    }

    @ParameterizedTest
    @EnumSource(CleaningStatus::class)
    fun `청소 상태와 완료 시각이 모순되는 저장은 DB 제약으로 거절한다`(status: CleaningStatus) {
        val reservation = reserve()
        val invalidCompletedAt = if (status == CleaningStatus.PENDING) Instant.parse("2026-09-28T05:00:00Z") else null
        assertFailsWith<DataIntegrityViolationException> {
            tasks.saveAndFlush(CleaningTask(reservation, status, invalidCompletedAt))
        }
        assertEquals(0L, tasks.count())
    }
}

class ControlledClock : Clock() {
    val current = AtomicReference(Instant.parse("2026-09-28T05:30:00Z"))
    val reads = AtomicInteger()
    private val barrier = AtomicReference<CyclicBarrier?>(null)

    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(current.get(), zone)

    override fun instant(): Instant {
        val index = reads.getAndIncrement()
        val activeBarrier = barrier.get() ?: return current.get()
        val value = current.get().plusSeconds(index.toLong())
        activeBarrier.await(10, TimeUnit.SECONDS)
        return value
    }

    fun overlapNextTwoReads() {
        reads.set(0)
        barrier.set(CyclicBarrier(2))
    }

    fun reset() {
        current.set(Instant.parse("2026-09-28T05:30:00Z"))
        reads.set(0)
        barrier.set(null)
    }
}
