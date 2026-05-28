package school.charset.app.domain.exercise

data class AttemptStep(
    val id: Long,
    val position: Int,
    val step: Step,
    val correct: Boolean,
    val errorType: String?,
    val attempts: Int,
    val revealed: Boolean,
    val userAnswer: Answer?,
)
