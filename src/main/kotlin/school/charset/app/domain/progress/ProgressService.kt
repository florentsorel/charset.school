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

    fun findAll(userId: Long): List<ModuleProgress> = progressRepository.findAllByUser(userId)
}
