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
import school.charset.app.domain.encoding.DecoderException
import school.charset.app.domain.encoding.EncoderException
import school.charset.app.domain.profile.CurrentPasswordMismatchException
import school.charset.app.domain.profile.PasswordConfirmationMismatchException
import school.charset.app.domain.profile.ProfileValidationKey
import school.charset.app.domain.sandbox.SandboxBytesParseException
import school.charset.app.domain.sandbox.SandboxEndianParseException
import school.charset.app.domain.sandbox.SandboxParseException
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

    @ExceptionHandler(CurrentPasswordMismatchException::class)
    fun handleCurrentPasswordMismatch(ex: CurrentPasswordMismatchException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "validation.failed",
                fieldErrors = mapOf(ex.field to listOf(ProfileValidationKey.CURRENT_PASSWORD_MISMATCH)),
            ),
        )

    @ExceptionHandler(PasswordConfirmationMismatchException::class)
    fun handlePasswordConfirmationMismatch(): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "validation.failed",
                fieldErrors = mapOf("confirmPassword" to listOf(ProfileValidationKey.PASSWORD_CONFIRM_MISMATCH)),
            ),
        )

    @ExceptionHandler(SandboxParseException::class)
    fun handleSandboxParse(ex: SandboxParseException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "sandbox.input-invalid",
                params = mapOf("reason" to ex.reason),
            ),
        )

    @ExceptionHandler(SandboxBytesParseException::class)
    fun handleSandboxBytesParse(ex: SandboxBytesParseException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "sandbox.bytes-invalid",
                params = mapOf("reason" to ex.reason),
            ),
        )

    @ExceptionHandler(SandboxEndianParseException::class)
    fun handleSandboxEndianParse(ex: SandboxEndianParseException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "sandbox.endian-invalid",
                params = mapOf("reason" to ex.reason),
            ),
        )

    @ExceptionHandler(EncoderException::class)
    fun handleEncoderException(ex: EncoderException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "encoding.not-encodable",
                params = mapOf("reason" to ex.reason),
            ),
        )

    @ExceptionHandler(DecoderException::class)
    fun handleDecoderException(ex: DecoderException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "encoding.not-decodable",
                params = mapOf("reason" to ex.reason),
            ),
        )

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
