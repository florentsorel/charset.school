package school.charset.app.infrastructure.http

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import school.charset.app.domain.auth.AuthErrorType
import school.charset.app.domain.auth.OrphanedSessionException
import school.charset.app.domain.user.EmailAlreadyTakenException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyTakenException::class)
    fun handleEmailAlreadyTaken(ex: EmailAlreadyTakenException): ResponseEntity<ErrorResponse> = ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
            errorType = AuthErrorType.EMAIL_ALREADY_TAKEN,
            params = mapOf("email" to ex.email),
        ),
    )

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(): ResponseEntity<ErrorResponse> = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
        ErrorResponse(errorType = AuthErrorType.BAD_CREDENTIALS),
    )

    @ExceptionHandler(OrphanedSessionException::class)
    fun handleOrphanedSession(
        ex: OrphanedSessionException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        request.getSession(false)?.invalidate()
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(errorType = AuthErrorType.SESSION_ORPHANED),
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors
            .groupBy { it.field }
            .mapValues { (_, errors) ->
                errors
                    .sortedBy { it.constraintRank() }
                    .map {
                        it.defaultMessage
                            ?: error("Bean Validation FieldError without message on '${it.field}'")
                    }
            }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
            ErrorResponse(errorType = "validation.failed", fieldErrors = fieldErrors),
        )
    }

    private fun FieldError.constraintRank(): Int = codes?.lastOrNull()?.let { CONSTRAINT_PRIORITY[it] } ?: Int.MAX_VALUE

    companion object {
        private val CONSTRAINT_PRIORITY = mapOf(
            "NotNull" to 0,
            "NotBlank" to 0,
            "NotEmpty" to 0,
            "Size" to 1,
            "Length" to 1,
            "Min" to 1,
            "Max" to 1,
            "Pattern" to 2,
            "Email" to 2,
        )
    }
}
