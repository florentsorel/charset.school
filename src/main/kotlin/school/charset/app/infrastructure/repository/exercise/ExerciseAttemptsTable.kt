package school.charset.app.infrastructure.repository.exercise

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.ExerciseModule

object ExerciseAttemptsTable : Table("exercise_attempts") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
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
