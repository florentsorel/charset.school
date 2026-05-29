package school.charset.app.infrastructure.http.exercise

import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer
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

data class ResumeExerciseResponse(
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
    val stepStates: List<StepStateDto>,
)

data class CurrentExerciseResponse(val attempt: ResumeExerciseResponse?)

data class StepStateDto(
    val correct: Boolean,
    val revealed: Boolean,
    val attempts: Int,
    val errorType: String?,
    val canReveal: Boolean,
    val userAnswer: RevealedAnswer?,
    val revealedAnswer: RevealedAnswer?,
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
    val count: Int? = null,
)

fun ExerciseAttempt.toResumeResponse(decodeBytes: List<Int>?): ResumeExerciseResponse {
    val direction = module.direction
    return ResumeExerciseResponse(
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
        stepStates = steps.map {
            StepStateDto(
                correct = it.correct,
                revealed = it.revealed,
                attempts = it.attempts,
                errorType = it.errorType,
                canReveal = !it.correct && !it.revealed && it.attempts >= ExerciseService.REVEAL_THRESHOLD,
                userAnswer = it.userAnswer?.toRevealedAnswer(),
                // Revealed steps echo the expected answer so a refreshed session
                // can render the revealed value (the DB never stores it as
                // `user_answer`; reveal only flips the `revealed` flag).
                revealedAnswer = if (it.revealed) it.step.toRevealedAnswer() else null,
            )
        },
    )
}

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

    is Step.UsefulBitCount -> RevealedAnswer(type = "useful-bit-count", count = expected)

    is Step.Endianness -> RevealedAnswer(
        type = "endianness",
        value = when (expected) {
            Encoding.Endian.BigEndian -> "big"
            Encoding.Endian.LittleEndian -> "little"
        },
    )
}

private fun Answer.toRevealedAnswer(): RevealedAnswer = when (this) {
    is Answer.FormatChoice -> RevealedAnswer(type = "format", value = value)

    is Answer.BinaryValue -> RevealedAnswer(type = "binary", value = bits)

    is Answer.BitGroupsValue -> RevealedAnswer(type = "bit-groups", groups = groups)

    is Answer.HexBytesValue -> RevealedAnswer(type = "hex-bytes", bytes = bytes)

    is Answer.CodePointValue -> RevealedAnswer(type = "code-point", codePoint = value)

    is Answer.UsefulBitCountValue -> RevealedAnswer(type = "useful-bit-count", count = value)

    is Answer.EndiannessChoice -> RevealedAnswer(
        type = "endianness",
        value = when (value) {
            Encoding.Endian.BigEndian -> "big"
            Encoding.Endian.LittleEndian -> "little"
        },
    )
}
