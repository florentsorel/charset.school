package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.generator.ExerciseGenerator
import school.charset.app.domain.progress.ProgressService
import kotlin.random.Random

class ExerciseService(
    private val exerciseGenerator: ExerciseGenerator,
    private val attemptRepository: ExerciseAttemptRepository,
    private val answerValidator: AnswerValidator,
    private val progressService: ProgressService,
    private val random: Random,
) {

    fun generate(token: String, module: ExerciseModule): ExerciseAttempt {
        // Level is driven by the visitor's persisted ModuleProgress (auto-
        // advanced by streak), not by the HTTP request. First-time visitors
        // start at level 1.
        val level = progressService.currentLevel(token, module)
        val encoding = pickEncoding(module)
        val exercise = when (module.direction) {
            ExerciseModule.Direction.Encode -> exerciseGenerator.generateEncode(encoding, level)
            ExerciseModule.Direction.Decode -> exerciseGenerator.generateDecode(encoding, level)
        }
        return attemptRepository.create(
            token = token,
            module = module,
            level = level,
            codePoint = exercise.codePoint,
            encoding = exercise.encoding,
            steps = exercise.steps,
        )
    }

    fun findResumable(token: String, module: ExerciseModule): ExerciseAttempt? = attemptRepository.findLatestUnfinalizedByTokenAndModule(token, module)

    fun validateStep(
        token: String,
        attemptId: Long,
        stepIndex: Int,
        answer: Answer,
    ): StepSubmissionOutcome {
        val attempt = loadAttempt(token, attemptId)
        if (attempt.finalized) throw AttemptAlreadyFinalizedException(attemptId)
        val targetStep = attempt.steps.getOrNull(stepIndex)
            ?: throw StepNotFoundException(attemptId, stepIndex)
        if (targetStep.correct || targetStep.revealed) {
            throw StepAlreadyResolvedException(attemptId, stepIndex)
        }

        val result = answerValidator.validate(targetStep.step, answer)
        val updatedStep = attemptRepository.recordStepSubmission(
            stepId = targetStep.id,
            userAnswer = answer,
            correct = result.ok,
            errorType = result.errorType,
        )

        val refreshed = attemptRepository.findById(attemptId)
            ?: error("Attempt $attemptId disappeared after step submission")
        val finalized = maybeFinalize(refreshed)

        return StepSubmissionOutcome(
            validation = result,
            step = updatedStep,
            attempt = finalized ?: refreshed,
            finalized = finalized != null,
        )
    }

    fun revealStep(
        token: String,
        attemptId: Long,
        stepIndex: Int,
    ): StepRevealOutcome {
        val attempt = loadAttempt(token, attemptId)
        if (attempt.finalized) throw AttemptAlreadyFinalizedException(attemptId)
        val targetStep = attempt.steps.getOrNull(stepIndex)
            ?: throw StepNotFoundException(attemptId, stepIndex)
        if (targetStep.correct || targetStep.revealed) {
            throw StepAlreadyResolvedException(attemptId, stepIndex)
        }

        if (targetStep.attempts < REVEAL_THRESHOLD) {
            throw RevealNotAllowedException(attemptId, stepIndex, targetStep.attempts, REVEAL_THRESHOLD)
        }

        val revealedStep = attemptRepository.markStepRevealed(targetStep.id)
        val refreshed = attemptRepository.findById(attemptId)
            ?: error("Attempt $attemptId disappeared after reveal")
        val finalized = maybeFinalize(refreshed)

        return StepRevealOutcome(
            step = revealedStep,
            attempt = finalized ?: refreshed,
            expected = targetStep.step,
        )
    }

    private fun loadAttempt(token: String, attemptId: Long): ExerciseAttempt {
        val attempt = attemptRepository.findById(attemptId) ?: throw AttemptNotFoundException(attemptId)
        // Sole ownership guard: an attempt is scoped to the visitor token that
        // created it. The token is an unguessable UUID from an HttpOnly cookie.
        if (attempt.token != token) throw AttemptNotFoundException(attemptId)
        return attempt
    }

    private fun maybeFinalize(attempt: ExerciseAttempt): ExerciseAttempt? {
        if (attempt.finalized) return null

        val allSubmitted = attempt.steps.all { it.correct || it.revealed }
        if (!allSubmitted) return null

        val attemptCorrect = attempt.steps.all { it.correct } && attempt.steps.none { it.revealed }
        val result = attemptRepository.finalize(attempt.id, correct = attemptCorrect, durationMs = null)
        progressService.recordCompletion(attempt.token, attempt.module, attemptCorrect)
        return result
    }

    private fun pickEncoding(module: ExerciseModule): Encoding = when (module) {
        ExerciseModule.Utf8Encode, ExerciseModule.Utf8Decode -> Encoding.Utf8
        ExerciseModule.Latin1Encode, ExerciseModule.Latin1Decode -> Encoding.Latin1
        ExerciseModule.Windows1252Encode, ExerciseModule.Windows1252Decode -> Encoding.Windows1252
        ExerciseModule.Utf16Encode, ExerciseModule.Utf16Decode ->
            if (random.nextBoolean()) Encoding.Utf16Be else Encoding.Utf16Le
        ExerciseModule.Utf32Encode, ExerciseModule.Utf32Decode ->
            if (random.nextBoolean()) Encoding.Utf32Be else Encoding.Utf32Le
    }

    companion object {
        const val REVEAL_THRESHOLD: Int = 3
    }
}

data class StepSubmissionOutcome(
    val validation: ValidationResult,
    val step: AttemptStep,
    val attempt: ExerciseAttempt,
    val finalized: Boolean,
)

data class StepRevealOutcome(
    val step: AttemptStep,
    val attempt: ExerciseAttempt,
    val expected: Step,
)

class AttemptNotFoundException(val attemptId: Long) : RuntimeException("Exercise attempt $attemptId not found")

class AttemptAlreadyFinalizedException(val attemptId: Long) : RuntimeException("Exercise attempt $attemptId is already finalized")

class StepAlreadyResolvedException(val attemptId: Long, val stepIndex: Int) : RuntimeException("Step $stepIndex of attempt $attemptId is already resolved (correct or revealed)")

class StepNotFoundException(val attemptId: Long, val stepIndex: Int) : RuntimeException("Step at index $stepIndex not found in attempt $attemptId")

class RevealNotAllowedException(
    val attemptId: Long,
    val stepIndex: Int,
    val currentAttempts: Int,
    val threshold: Int,
) : RuntimeException(
    "Cannot reveal step $stepIndex of attempt $attemptId: needs $threshold submissions, got $currentAttempts",
)
