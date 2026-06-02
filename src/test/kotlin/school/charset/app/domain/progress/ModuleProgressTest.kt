package school.charset.app.domain.progress

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.exercise.ExerciseModule
import kotlin.time.Instant

class ModuleProgressTest :
    FreeSpec({
        val now = Instant.parse("2026-01-01T00:00:00Z")

        // Utf8Encode caps at level 4, so there is headroom to test mid-ladder
        // advancement as well as the ceiling.
        fun progress(level: Int, streak: Int): ModuleProgress = ModuleProgress(
            token = "tok-1",
            module = ExerciseModule.Utf8Encode,
            level = level,
            streak = streak,
            attempts = 0,
            errors = 0,
            lastPlayedAt = null,
        )

        "recordCompletion" - {
            "correct below threshold increments streak, keeps level" {
                val result = progress(level = 1, streak = 3).recordCompletion(correct = true, now = now)

                result.streak shouldBe 4
                result.level shouldBe 1
                result.attempts shouldBe 1
                result.errors shouldBe 0
                result.lastPlayedAt shouldBe now
            }

            "correct hitting threshold advances level and resets streak" {
                // streak 4 + this correct = 5 = STREAK_FOR_LEVEL_UP
                val result = progress(level = 1, streak = 4).recordCompletion(correct = true, now = now)

                result.level shouldBe 2
                result.streak shouldBe 0
                result.attempts shouldBe 1
            }

            "incorrect resets streak and records an error, keeps level" {
                val result = progress(level = 2, streak = 4).recordCompletion(correct = false, now = now)

                result.streak shouldBe 0
                result.level shouldBe 2
                result.errors shouldBe 1
                result.attempts shouldBe 1
            }

            "at max level, hitting the threshold keeps the streak growing (no advance)" {
                // Utf8Encode maxLevel = 4. Already at 4: no further level-up,
                // and the streak is NOT reset so the cap doesn't punish a run.
                val result = progress(level = 4, streak = 4).recordCompletion(correct = true, now = now)

                result.level shouldBe 4
                result.streak shouldBe 5
            }
        }
    })
