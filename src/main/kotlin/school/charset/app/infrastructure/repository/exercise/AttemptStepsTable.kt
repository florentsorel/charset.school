package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import school.charset.app.domain.exercise.AttemptStep
import school.charset.app.domain.exercise.StepType

object AttemptStepsTable : Table("attempt_steps") {
    val id = long("id").autoIncrement()
    val attemptId = long("attempt_id").references(ExerciseAttemptsTable.id)
    val position = short("position")
    val stepType = varchar("step_type", 32).transform(
        wrap = { StepType.fromId(it) ?: error("Unknown step type from DB: $it") },
        unwrap = { it.id },
    )
    val correct = bool("correct")
    val errorType = varchar("error_type", 64).nullable()
    val attempts = short("attempts")
    val revealed = bool("revealed")

    override val primaryKey = PrimaryKey(id)
}

// Lets a caller skip an extra SELECT after its own UPDATE by passing in
// the values it just wrote. Used by recordStepSubmission.
data class StepRowOverrides(
    val correct: Boolean,
    val errorType: String?,
    val attempts: Int,
)

// Builds an AttemptStep from an already-fetched parent row + 1 child read.
// Reuses the batched child-loader so single-step paths keep one query
// (one read per call, regardless of the step type).
fun ResultRow.toAttemptStep(overrides: StepRowOverrides? = null): AttemptStep {
    val stepId = this[AttemptStepsTable.id]
    val stepType = this[AttemptStepsTable.stepType]
    val (step, answer) = selectChildRows(stepType, listOf(stepId))
        .singleOrNull()
        ?.second
        ?: error("Missing child row for step $stepId of type $stepType")
    return AttemptStep(
        id = stepId,
        position = this[AttemptStepsTable.position].toInt(),
        step = step,
        correct = overrides?.correct ?: this[AttemptStepsTable.correct],
        errorType = overrides?.errorType ?: this[AttemptStepsTable.errorType],
        attempts = overrides?.attempts ?: this[AttemptStepsTable.attempts].toInt(),
        revealed = this[AttemptStepsTable.revealed],
        userAnswer = answer,
    )
}
