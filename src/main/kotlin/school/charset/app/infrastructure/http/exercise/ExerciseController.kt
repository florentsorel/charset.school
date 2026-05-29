package school.charset.app.infrastructure.http.exercise

import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.ExerciseService
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

    @GetMapping("/current")
    fun current(
        authentication: Authentication,
        @RequestParam moduleId: String,
    ): ResponseEntity<CurrentExerciseResponse> {
        val userId = authentication.requireUserDetailsAdapter().userId
        val module = ExerciseModule.fromId(moduleId) ?: throw UnknownModuleException(moduleId)

        val attempt = exerciseService.findResumable(userId, module)
            ?: return ResponseEntity.ok(CurrentExerciseResponse(attempt = null))

        val decodeBytes = if (module.direction == ExerciseModule.Direction.Decode) {
            codec.encode(attempt.codePoint, attempt.encoding).map { it.toInt() and 0xFF }
        } else {
            null
        }
        return ResponseEntity.ok(CurrentExerciseResponse(attempt = attempt.toResumeResponse(decodeBytes)))
    }

    @PostMapping("/generate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun generate(
        authentication: Authentication,
        @Valid @RequestBody request: GenerateExerciseRequest,
    ): ResponseEntity<GenerateExerciseResponse> {
        val userId = authentication.requireUserDetailsAdapter().userId
        val module = ExerciseModule.fromId(request.moduleId)
            ?: throw UnknownModuleException(request.moduleId)

        val attempt = exerciseService.generate(userId, module, request.level)
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
