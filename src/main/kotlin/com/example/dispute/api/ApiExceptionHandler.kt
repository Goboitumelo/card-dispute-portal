package com.example.dispute.api

import com.example.dispute.service.ConflictException
import com.example.dispute.service.NotFoundException
import com.example.dispute.service.UnprocessableException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    /**
     * Maps [NotFoundException] to 404.
     *
     * @param ex the exception to map
     * @return a problem response carrying the exception message
     */
    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, ex.message)

    /**
     * Maps [ConflictException] to 409.
     *
     * @param ex the exception to map
     * @return a problem response carrying the exception message
     */
    @ExceptionHandler(ConflictException::class)
    fun conflict(ex: ConflictException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, ex.message)

    /**
     * Maps [UnprocessableException] to 422.
     *
     * @param ex the exception to map
     * @return a problem response carrying the exception message
     */
    @ExceptionHandler(UnprocessableException::class)
    fun unprocessable(ex: UnprocessableException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNPROCESSABLE_CONTENT, ex.message)

    /**
     * Maps an optimistic locking failure to 409, asking the client to reload.
     *
     * @param ex the exception to map
     * @return a problem response with a reload message
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun concurrentUpdate(ex: ObjectOptimisticLockingFailureException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "The dispute was changed by another request. Reload and try again.")

    /**
     * Builds a problem-details response.
     *
     * @param status the HTTP status to send
     * @param detail the detail message, or null
     * @return the response
     */
    private fun problem(status: HttpStatus, detail: String?): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail))
}
