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
import school.charset.app.domain.exercise.AttemptAlreadyFinalizedException
import school.charset.app.domain.exercise.AttemptNotFoundException
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.RevealNotAllowedException
import school.charset.app.domain.exercise.StepAlreadyResolvedException
import school.charset.app.domain.exercise.StepNotFoundException
import school.charset.app.domain.profile.CurrentPasswordMismatchException
import school.charset.app.domain.profile.PasswordConfirmationMismatchException
import school.charset.app.domain.profile.ProfileValidationKey
import school.charset.app.domain.sandbox.SandboxBytesParseException
import school.charset.app.domain.sandbox.SandboxEndianParseException
import school.charset.app.domain.sandbox.SandboxParseException
import school.charset.app.domain.user.EmailAlreadyTakenException
import school.charset.app.infrastructure.http.exercise.InvalidAnswerPayloadException
import school.charset.app.infrastructure.http.exercise.UnknownGranularityException
import school.charset.app.infrastructure.http.exercise.UnknownModuleException

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

    @ExceptionHandler(UnknownModuleException::class)
    fun handleUnknownModule(ex: UnknownModuleException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse(errorType = "exercise.unknown-module", params = mapOf("moduleId" to ex.moduleId)))

    @ExceptionHandler(UnknownGranularityException::class)
    fun handleUnknownGranularity(ex: UnknownGranularityException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse(errorType = "exercise.unknown-granularity", params = mapOf("granularity" to ex.granularity)))

    @ExceptionHandler(AttemptNotFoundException::class)
    fun handleAttemptNotFound(ex: AttemptNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse(errorType = "exercise.attempt-not-found", params = mapOf("attemptId" to ex.attemptId.toString())))

    @ExceptionHandler(StepNotFoundException::class)
    fun handleStepNotFound(ex: StepNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            ErrorResponse(
                errorType = "exercise.step-not-found",
                params = mapOf(
                    "attemptId" to ex.attemptId.toString(),
                    "stepIndex" to ex.stepIndex.toString(),
                ),
            ),
        )

    @ExceptionHandler(AttemptAlreadyFinalizedException::class)
    fun handleAttemptAlreadyFinalized(ex: AttemptAlreadyFinalizedException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse(errorType = "exercise.attempt-already-finalized", params = mapOf("attemptId" to ex.attemptId.toString())))

    @ExceptionHandler(StepAlreadyResolvedException::class)
    fun handleStepAlreadyResolved(ex: StepAlreadyResolvedException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "exercise.step-already-resolved",
                params = mapOf(
                    "attemptId" to ex.attemptId.toString(),
                    "stepIndex" to ex.stepIndex.toString(),
                ),
            ),
        )

    @ExceptionHandler(InvalidAnswerPayloadException::class)
    fun handleInvalidAnswerPayload(ex: InvalidAnswerPayloadException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "exercise.invalid-answer-payload",
                params = mapOf(
                    "answerType" to ex.answerType,
                    "reason" to ex.reason,
                ),
            ),
        )

    @ExceptionHandler(RevealNotAllowedException::class)
    fun handleRevealNotAllowed(ex: RevealNotAllowedException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "exercise.reveal-not-allowed",
                params = mapOf(
                    "attempts" to ex.currentAttempts.toString(),
                    "threshold" to ex.threshold.toString(),
                ),
            ),
        )

    @ExceptionHandler(ExerciseGenerationException::class)
    fun handleExerciseGeneration(ex: ExerciseGenerationException): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(
            ErrorResponse(
                errorType = "exercise.generation-failed",
                params = mapOf(
                    "encoding" to ex.encoding.id,
                    "level" to ex.level.toString(),
                    "reason" to ex.reason,
                ),
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
