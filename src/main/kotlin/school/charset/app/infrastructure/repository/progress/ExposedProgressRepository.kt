package school.charset.app.infrastructure.repository.progress

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.progress.ModuleProgress
import school.charset.app.domain.progress.ProgressRepository
import kotlin.time.Clock

class ExposedProgressRepository(
    private val clock: Clock,
) : ProgressRepository {

    override fun findByUserAndModule(userId: Long, module: ExerciseModule): ModuleProgress? = transaction {
        ModuleProgressTable
            .selectAll()
            .where { (ModuleProgressTable.userId eq userId) and (ModuleProgressTable.moduleId eq module) }
            .singleOrNull()
            ?.toModuleProgress()
    }

    override fun findAllByUser(userId: Long): List<ModuleProgress> = transaction {
        ModuleProgressTable
            .selectAll()
            .where { ModuleProgressTable.userId eq userId }
            .map { it.toModuleProgress() }
    }

    override fun upsert(progress: ModuleProgress): ModuleProgress = transaction {
        val now = clock.now()
        val existing = ModuleProgressTable
            .selectAll()
            .where { (ModuleProgressTable.userId eq progress.userId) and (ModuleProgressTable.moduleId eq progress.module) }
            .singleOrNull()

        if (existing == null) {
            ModuleProgressTable.insert {
                it[userId] = progress.userId
                it[moduleId] = progress.module
                it[level] = progress.level.toShort()
                it[streak] = progress.streak
                it[attempts] = progress.attempts
                it[errors] = progress.errors
                it[lastPlayedAt] = progress.lastPlayedAt
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            ModuleProgressTable.update(
                { (ModuleProgressTable.userId eq progress.userId) and (ModuleProgressTable.moduleId eq progress.module) },
            ) {
                it[level] = progress.level.toShort()
                it[streak] = progress.streak
                it[attempts] = progress.attempts
                it[errors] = progress.errors
                it[lastPlayedAt] = progress.lastPlayedAt
                it[updatedAt] = now
            }
        }

        progress
    }
}
