package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.AttemptStep
import school.charset.app.domain.exercise.ExerciseAttempt
import school.charset.app.domain.exercise.ExerciseModule

object ExerciseAttemptsTable : Table("exercise_attempts") {
    val id = long("id").autoIncrement()
    val token = varchar("token", 64)
    val moduleId = varchar("module_id", 64).transform(
        wrap = { ExerciseModule.fromId(it) ?: error("Unknown module id from DB: $it") },
        unwrap = { it.id },
    )
    val level = short("level")
    val codePoint = integer("code_point")
    val encoding = varchar("encoding", 16).transform(
        wrap = { Encoding.fromId(it) ?: error("Unknown encoding from DB: $it") },
        unwrap = { it.id },
    )
    val correct = bool("correct")
    val finalized = bool("finalized")
    val durationMs = integer("duration_ms").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

// Hydrates an ExerciseAttempt from its parent row, batching child reads
// by step type:
//   1 SELECT attempt_steps + 1 SELECT per distinct step_type in the
//   attempt (each fetches expected + user_answer in one row).
// For a typical 5-step UTF-8 attempt (5 distinct types): 6 queries.
fun ResultRow.toExerciseAttempt(): ExerciseAttempt {
    val attemptId = this[ExerciseAttemptsTable.id]

    val stepRows = AttemptStepsTable
        .selectAll()
        .where { AttemptStepsTable.attemptId eq attemptId }
        .orderBy(AttemptStepsTable.position, SortOrder.ASC)
        .toList()

    val dataByStepId = loadStepDataBatched(stepRows)

    val attemptSteps = stepRows.map { row ->
        val stepId = row[AttemptStepsTable.id]
        val (step, answer) = dataByStepId[stepId]
            ?: error("Missing child row for step $stepId in attempt $attemptId")
        AttemptStep(
            id = stepId,
            position = row[AttemptStepsTable.position].toInt(),
            step = step,
            correct = row[AttemptStepsTable.correct],
            errorType = row[AttemptStepsTable.errorType],
            attempts = row[AttemptStepsTable.attempts].toInt(),
            revealed = row[AttemptStepsTable.revealed],
            userAnswer = answer,
        )
    }

    return ExerciseAttempt(
        id = attemptId,
        token = this[ExerciseAttemptsTable.token],
        module = this[ExerciseAttemptsTable.moduleId],
        level = this[ExerciseAttemptsTable.level].toInt(),
        codePoint = CodePoint(this[ExerciseAttemptsTable.codePoint]),
        encoding = this[ExerciseAttemptsTable.encoding],
        correct = this[ExerciseAttemptsTable.correct],
        finalized = this[ExerciseAttemptsTable.finalized],
        durationMs = this[ExerciseAttemptsTable.durationMs],
        steps = attemptSteps,
        createdAt = this[ExerciseAttemptsTable.createdAt],
    )
}
