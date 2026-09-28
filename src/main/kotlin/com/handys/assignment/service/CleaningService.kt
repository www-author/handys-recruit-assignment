package com.handys.assignment.service

import com.handys.assignment.domain.CleaningStatus
import com.handys.assignment.repository.CleaningTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

data class CleaningCompletion(val id: Long, val status: CleaningStatus, val completedAt: Instant)

class CleaningTaskNotFoundException : RuntimeException("청소 작업을 찾을 수 없습니다.")

@Service
class CleaningService(private val tasks: CleaningTaskRepository, private val clock: Clock) {
    @Transactional
    fun complete(id: Long): CleaningCompletion {
        val now = clock.instant()
        // 첫 상태 전이에 성공한 요청만 완료 시각을 기록한다. 후속 요청은 저장된 값을 반환한다.
        tasks.completeIfPending(id, now, CleaningStatus.COMPLETED, CleaningStatus.PENDING)
        val task = tasks.findById(id).orElseThrow { CleaningTaskNotFoundException() }
        return CleaningCompletion(id, task.status, requireNotNull(task.completedAt))
    }
}
