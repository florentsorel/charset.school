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
    // Capped by the encoding's actual max level (mirrors Utf8Level /
    // Utf16Level / etc. via ExerciseModule.maxLevel), so the field never
    // suggests a level the generator would refuse.
    val suggestedLevel: Int
        get() = if (streak >= STREAK_FOR_LEVEL_UP && level < module.maxLevel) level + 1 else level

    fun recordCompletion(correct: Boolean, now: Instant): ModuleProgress = copy(
        attempts = attempts + 1,
        errors = if (correct) errors else errors + 1,
        streak = if (correct) streak + 1 else 0,
        lastPlayedAt = now,
    )

    companion object {
        const val STREAK_FOR_LEVEL_UP: Int = 5

        fun initial(userId: Long, module: ExerciseModule): ModuleProgress = ModuleProgress(userId, module, level = 1, streak = 0, attempts = 0, errors = 0, lastPlayedAt = null)
    }
}
