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
    // First-time users (no row in module_progress) start at level 1.
    fun currentLevel(userId: Long, module: ExerciseModule): Int = progressRepository.findByUserAndModule(userId, module)?.level ?: 1

    fun findAll(userId: Long): List<ModuleProgress> = progressRepository.findAllByUser(userId)
}
