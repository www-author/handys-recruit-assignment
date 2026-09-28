package com.handys.assignment.api

import com.handys.assignment.service.CleaningTaskNotFoundException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ApiError(val code: String, val message: String)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(CleaningTaskNotFoundException::class)
    fun notFound(): ResponseEntity<ApiError> = ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError("CLEANING_TASK_NOT_FOUND", "청소 작업을 찾을 수 없습니다."))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, HandlerMethodValidationException::class, ConstraintViolationException::class)
    fun invalidId(): ResponseEntity<ApiError> = ResponseEntity.badRequest()
        .body(ApiError("INVALID_TASK_ID", "청소 작업 ID는 양의 Long 범위 정수여야 합니다."))
}
