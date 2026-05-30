package school.charset.app.domain.progress

import school.charset.app.domain.exercise.ExerciseModule
import kotlin.time.Instant

data class ModuleProgress(
    val userId: Long,
    val module: ExerciseModule,
    val level: Int,
    val streak: Int,
    val attempts: Int,
    val errors: Int,
    val lastPlayedAt: Instant?,
) {
    // Auto-advances the level once the streak threshold is hit (while still
    // below the module's max level), then resets the streak so the user has
    // to earn the next bump as well. The level is no longer user-selectable
    // from the UI - the back drives progression entirely.
    fun recordCompletion(correct: Boolean, now: Instant): ModuleProgress {
        val updated = copy(
            attempts = attempts + 1,
            errors = if (correct) errors else errors + 1,
            streak = if (correct) streak + 1 else 0,
            lastPlayedAt = now,
        )
        return if (updated.streak >= STREAK_FOR_LEVEL_UP && updated.level < module.maxLevel) {
            updated.copy(level = updated.level + 1, streak = 0)
        } else {
            updated
        }
    }

    companion object {
        const val STREAK_FOR_LEVEL_UP: Int = 5

        fun initial(userId: Long, module: ExerciseModule): ModuleProgress = ModuleProgress(userId, module, level = 1, streak = 0, attempts = 0, errors = 0, lastPlayedAt = null)
    }
}
