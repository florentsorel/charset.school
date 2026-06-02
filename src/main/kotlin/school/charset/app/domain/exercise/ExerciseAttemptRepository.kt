package school.charset.app.domain.exercise

interface ExerciseAttemptRepository {
    fun create(
        token: String,
        module: ExerciseModule,
        level: Int,
        codePoint: school.charset.app.domain.encoding.CodePoint,
        encoding: school.charset.app.domain.encoding.Encoding,
        steps: List<Step>,
    ): ExerciseAttempt

    fun findById(attemptId: Long): ExerciseAttempt?

    fun findLatestUnfinalizedByTokenAndModule(token: String, module: ExerciseModule): ExerciseAttempt?

    fun recordStepSubmission(
        stepId: Long,
        userAnswer: Answer,
        correct: Boolean,
        errorType: String?,
    ): AttemptStep

    fun markStepRevealed(stepId: Long): AttemptStep

    fun finalize(attemptId: Long, correct: Boolean, durationMs: Int?): ExerciseAttempt
}
