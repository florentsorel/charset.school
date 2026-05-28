package school.charset.app.infrastructure.http.exercise

import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.ExerciseAttempt
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.ExerciseService
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.exercise.StepRevealOutcome
import school.charset.app.domain.exercise.StepSubmissionOutcome

data class GenerateExerciseResponse(
    val attemptId: Long,
    val moduleId: String,
    val direction: String,
    val level: Int,
    val granularity: String,
    val encoding: String,
    val codePoint: Int?,
    val codePointLabel: String?,
    val bytes: List<Int>?,
    val steps: List<ExerciseStepDto>,
)

data class ValidateStepResponse(
    val ok: Boolean,
    val errorType: String?,
    val params: Map<String, String>,
    val attempts: Int,
    val canReveal: Boolean,
    val attemptFinalized: Boolean,
    val attemptCorrect: Boolean,
)

data class RevealStepResponse(
    val stepIndex: Int,
    val attempts: Int,
    val answer: RevealedAnswer,
    val attemptFinalized: Boolean,
    val attemptCorrect: Boolean,
)

data class RevealedAnswer(
    val type: String,
    val value: String? = null,
    val groups: List<String>? = null,
    val bytes: List<Int>? = null,
    val codePoint: Int? = null,
)

fun ExerciseAttempt.toGenerateResponse(decodeBytes: List<Int>?): GenerateExerciseResponse {
    val direction = module.direction
    return GenerateExerciseResponse(
        attemptId = id,
        moduleId = module.id,
        direction = direction.name.lowercase(),
        level = level,
        granularity = granularity.id,
        encoding = encoding.id,
        codePoint = if (direction == ExerciseModule.Direction.Encode) codePoint.value else null,
        codePointLabel = if (direction == ExerciseModule.Direction.Encode) codePoint.toString() else null,
        bytes = if (direction == ExerciseModule.Direction.Decode) decodeBytes else null,
        steps = steps.map { it.step.toDto() },
    )
}

fun StepSubmissionOutcome.toResponse(): ValidateStepResponse = ValidateStepResponse(
    ok = validation.ok,
    errorType = validation.errorType,
    params = validation.params,
    attempts = step.attempts,
    canReveal = step.attempts >= ExerciseService.REVEAL_THRESHOLD,
    attemptFinalized = finalized,
    attemptCorrect = attempt.correct,
)

fun StepRevealOutcome.toResponse(): RevealStepResponse = RevealStepResponse(
    stepIndex = step.position,
    attempts = step.attempts,
    answer = expected.toRevealedAnswer(),
    attemptFinalized = attempt.steps.all { it.correct || it.revealed },
    attemptCorrect = attempt.correct,
)

private fun Step.toRevealedAnswer(): RevealedAnswer = when (this) {
    is Step.Format -> RevealedAnswer(type = "format", value = expected)
    is Step.Binary -> RevealedAnswer(type = "binary", value = expected)
    is Step.BitGroups -> RevealedAnswer(type = "bit-groups", groups = expected)
    is Step.HexBytes -> RevealedAnswer(type = "hex-bytes", bytes = expected)
    is Step.CodePointEntry -> RevealedAnswer(type = "code-point", codePoint = expected)
    is Step.Endianness -> RevealedAnswer(
        type = "endianness",
        value = when (expected) {
            Encoding.Endian.BigEndian -> "big"
            Encoding.Endian.LittleEndian -> "little"
        },
    )
}
