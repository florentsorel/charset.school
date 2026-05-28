package school.charset.app.domain.progress

import school.charset.app.domain.exercise.ExerciseModule

interface ProgressRepository {
    fun findByUserAndModule(userId: Long, module: ExerciseModule): ModuleProgress?
    fun findAllByUser(userId: Long): List<ModuleProgress>
    fun upsert(progress: ModuleProgress): ModuleProgress
}
