package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.Table
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
