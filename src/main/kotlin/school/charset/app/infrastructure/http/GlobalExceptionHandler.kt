package school.charset.app.infrastructure.http

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import school.charset.app.domain.encoding.DecoderException
import school.charset.app.domain.encoding.EncoderException
import school.charset.app.domain.exercise.AttemptAlreadyFinalizedException
import school.charset.app.domain.exercise.AttemptNotFoundException
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.RevealNotAllowedException
import school.charset.app.domain.exercise.StepAlreadyResolvedException
import school.charset.app.domain.exercise.StepNotFoundException
import school.charset.app.domain.sandbox.SandboxBytesParseException
import school.charset.app.domain.sandbox.SandboxEndianParseException
import school.charset.app.domain.sandbox.SandboxParseException
import school.charset.app.infrastructure.http.exercise.InvalidAnswerPayloadException
import school.charset.app.infrastructure.http.exercise.UnknownModuleException

// Extends ResponseEntityExceptionHandler so all standard Spring MVC framework
// exceptions (wrong method, unsupported media type, type mismatch, missing
// params, unreadable body, ...) keep their proper 4xx status instead of being
// swallowed by the catch-all below as a 500. We override handleExceptionInternal
// to render those in our ErrorResponse shape rather than the default RFC-7807
// ProblemDetail.
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MissingTokenIdException::class)
    fun handleMissingTokenId(): ResponseEntity<ErrorResponse> {
        // No token_id cookie reached the API. The cookie is minted at the Nuxt
        // edge, so this only happens on a direct (non-browser) call - a clear
        // contract violation, not a silently-created identity.
        log.warn("Request without token_id cookie")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(errorType = "token-id.missing"),
        )
    }

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

    // Bean Validation failures. Kept at 422 (not the framework default 400) to
    // preserve existing client behavior, with per-field i18n keys.
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
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

    // Renders every other framework exception (handled by the base class:
    // unreadable body, wrong method, unsupported media type, type mismatch,
    // missing params, 404, ...) in our ErrorResponse shape, keeping the
    // framework-computed status. `errorType` is derived from that status.
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errorType = when (HttpStatus.resolve(statusCode.value())) {
            HttpStatus.BAD_REQUEST -> "request.bad"
            HttpStatus.NOT_FOUND -> "request.not-found"
            HttpStatus.METHOD_NOT_ALLOWED -> "request.method-not-allowed"
            HttpStatus.NOT_ACCEPTABLE -> "request.not-acceptable"
            HttpStatus.UNSUPPORTED_MEDIA_TYPE -> "request.unsupported-media-type"
            else -> "request.error"
        }
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse(errorType = errorType))
    }

    // Last-resort handler: anything not matched above (and not a framework
    // exception handled by the base class) is an unexpected/technical failure.
    // Log the full stack trace (we're otherwise blind in prod) and return a
    // generic 500 - never leak internals to the client.
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(errorType = "internal.server-error"))
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
