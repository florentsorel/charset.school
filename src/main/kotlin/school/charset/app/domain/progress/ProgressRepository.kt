package school.charset.app.domain.progress

import school.charset.app.domain.exercise.ExerciseModule

interface ProgressRepository {
    fun findByTokenAndModule(token: String, module: ExerciseModule): ModuleProgress?
    fun findAllByToken(token: String): List<ModuleProgress>
    fun upsert(progress: ModuleProgress): ModuleProgress
}
