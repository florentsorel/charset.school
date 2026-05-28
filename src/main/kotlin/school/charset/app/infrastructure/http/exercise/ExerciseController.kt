package school.charset.app.infrastructure.http.exercise

import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.ExerciseService
import school.charset.app.domain.exercise.Granularity
import school.charset.app.infrastructure.security.requireUserDetailsAdapter

@RestController
@RequestMapping(
    "/api/exercise",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class ExerciseController(
    private val exerciseService: ExerciseService,
    private val codec: Codec,
) {

    @PostMapping("/generate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun generate(
        authentication: Authentication,
        @Valid @RequestBody request: GenerateExerciseRequest,
    ): ResponseEntity<GenerateExerciseResponse> {
        val userId = authentication.requireUserDetailsAdapter().userId
        val module = ExerciseModule.fromId(request.moduleId)
            ?: throw UnknownModuleException(request.moduleId)
        val granularity = Granularity.fromId(request.granularity)
            ?: throw UnknownGranularityException(request.granularity)

        val attempt = exerciseService.generate(userId, module, request.level, granularity)
        val decodeBytes = if (module.direction == ExerciseModule.Direction.Decode) {
            codec.encode(attempt.codePoint, attempt.encoding).map { it.toInt() and 0xFF }
        } else {
            null
        }
        return ResponseEntity.ok(attempt.toGenerateResponse(decodeBytes))
    }

    @PostMapping("/validate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun validate(
        authentication: Authentication,
        @Valid @RequestBody request: ValidateStepRequest,
    ): ResponseEntity<ValidateStepResponse> {
        val userId = authentication.requireUserDetailsAdapter().userId
        val outcome = exerciseService.validateStep(
            userId = userId,
            attemptId = request.attemptId,
            stepIndex = request.stepIndex,
            answer = request.answer.toDomain(),
        )
        return ResponseEntity.ok(outcome.toResponse())
    }

    @PostMapping("/reveal", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun reveal(
        authentication: Authentication,
        @Valid @RequestBody request: RevealStepRequest,
    ): ResponseEntity<RevealStepResponse> {
        val userId = authentication.requireUserDetailsAdapter().userId
        val outcome = exerciseService.revealStep(
            userId = userId,
            attemptId = request.attemptId,
            stepIndex = request.stepIndex,
        )
        return ResponseEntity.ok(outcome.toResponse())
    }
}

class UnknownModuleException(val moduleId: String) : RuntimeException("Unknown exercise module: $moduleId")

class UnknownGranularityException(val granularity: String) : RuntimeException("Unknown granularity: $granularity")
