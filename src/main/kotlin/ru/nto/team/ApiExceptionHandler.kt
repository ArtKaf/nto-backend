package ru.nto.team

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

class ApiException(val status: Int, message: String) : RuntimeException(message)
class ValidationException(val errors: Map<String, String>) : RuntimeException()

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun apiError(error: ApiException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(error.status).body(mapOf("error" to error.message!!))

    @ExceptionHandler(ValidationException::class)
    fun validationError(error: ValidationException): ResponseEntity<Map<String, Map<String, String>>> =
        ResponseEntity.badRequest().body(mapOf("errors" to error.errors))
}
