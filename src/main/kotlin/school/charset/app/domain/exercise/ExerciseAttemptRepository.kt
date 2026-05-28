package school.charset.app.domain.exercise

interface ExerciseAttemptRepository {
    fun create(
        userId: Long,
        module: ExerciseModule,
        level: Int,
        granularity: Granularity,
        codePoint: school.charset.app.domain.encoding.CodePoint,
        encoding: school.charset.app.domain.encoding.Encoding,
        steps: List<Step>,
    ): ExerciseAttempt

    fun findById(attemptId: Long): ExerciseAttempt?

    fun recordStepSubmission(
        stepId: Long,
        userAnswer: Answer,
        correct: Boolean,
        errorType: String?,
    ): AttemptStep

    fun markStepRevealed(stepId: Long): AttemptStep

    fun finalize(attemptId: Long, correct: Boolean, durationMs: Int?): ExerciseAttempt
}
