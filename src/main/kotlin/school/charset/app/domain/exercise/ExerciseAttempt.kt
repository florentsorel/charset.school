package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import kotlin.time.Instant

data class ExerciseAttempt(
    val id: Long,
    val userId: Long,
    val module: ExerciseModule,
    val level: Int,
    val codePoint: CodePoint,
    val encoding: Encoding,
    val correct: Boolean,
    val finalized: Boolean,
    val durationMs: Int?,
    val steps: List<AttemptStep>,
    val createdAt: Instant,
)
