package school.charset.app.domain.progress

import school.charset.app.domain.exercise.ExerciseModule
import kotlin.time.Clock

class ProgressService(
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
) {
    fun recordCompletion(userId: Long, module: ExerciseModule, correct: Boolean): ModuleProgress {
        val current = progressRepository.findByUserAndModule(userId, module)
            ?: ModuleProgress.initial(userId, module)
        return progressRepository.upsert(current.recordCompletion(correct, clock.now()))
    }

    // Used by ExerciseService.generate to pick the level for a new attempt.
    // First-time users (no row in module_progress) start at level 1. Clamped
    // to 1..maxLevel defensively: this is now the single source of truth for
    // generation, so a stray out-of-range value (legacy data / manual edit)
    // must not make the generator throw.
    fun currentLevel(userId: Long, module: ExerciseModule): Int {
        val level = progressRepository.findByUserAndModule(userId, module)?.level ?: 1
        return level.coerceIn(1, module.maxLevel)
    }

    fun findAll(userId: Long): List<ModuleProgress> = progressRepository.findAllByUser(userId)
}
