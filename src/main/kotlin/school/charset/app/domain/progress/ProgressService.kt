package school.charset.app.domain.progress

import school.charset.app.domain.exercise.ExerciseModule
import kotlin.time.Clock

class ProgressService(
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
) {
    fun recordCompletion(token: String, module: ExerciseModule, correct: Boolean): ModuleProgress {
        val current = progressRepository.findByTokenAndModule(token, module)
            ?: ModuleProgress.initial(token, module)
        return progressRepository.upsert(current.recordCompletion(correct, clock.now()))
    }

    // Used by ExerciseService.generate to pick the level for a new attempt.
    // First-time users (no row in module_progress) start at level 1. Clamped
    // to 1..maxLevel defensively: this is now the single source of truth for
    // generation, so a stray out-of-range value (legacy data / manual edit)
    // must not make the generator throw.
    fun currentLevel(token: String, module: ExerciseModule): Int {
        val level = progressRepository.findByTokenAndModule(token, module)?.level ?: 1
        return level.coerceIn(1, module.maxLevel)
    }

    fun findAll(token: String): List<ModuleProgress> = progressRepository.findAllByToken(token)
}
