package school.charset.app.infrastructure.repository.progress

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.progress.ModuleProgress

object ModuleProgressTable : Table("module_progress") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val moduleId = varchar("module_id", 64).transform(
        wrap = { ExerciseModule.fromId(it) ?: error("Unknown module id from DB: $it") },
        unwrap = { it.id },
    )
    val level = short("level")
    val streak = integer("streak")
    val attempts = integer("attempts")
    val errors = integer("errors")
    val lastPlayedAt = timestamp("last_played_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toModuleProgress(): ModuleProgress = ModuleProgress(
    userId = this[ModuleProgressTable.userId],
    module = this[ModuleProgressTable.moduleId],
    level = this[ModuleProgressTable.level].toInt(),
    streak = this[ModuleProgressTable.streak],
    attempts = this[ModuleProgressTable.attempts],
    errors = this[ModuleProgressTable.errors],
    lastPlayedAt = this[ModuleProgressTable.lastPlayedAt],
)
