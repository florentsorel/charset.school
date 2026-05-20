package school.charset.app.domain.exercise

@ConsistentCopyVisibility
data class ValidationResult private constructor(
    val ok: Boolean,
    val errorType: String? = null,
    val params: Map<String, String> = emptyMap(),
) {
    init {
        if (ok) {
            require(errorType == null) { "Correct result must not carry an errorType" }
            require(params.isEmpty()) { "Correct result must not carry params" }
        } else {
            require(errorType != null) { "Incorrect result must carry an errorType" }
        }
    }

    companion object {
        fun correct(): ValidationResult = ValidationResult(ok = true)

        fun incorrect(
            errorType: String,
            params: Map<String, String> = emptyMap(),
        ): ValidationResult = ValidationResult(
            ok = false,
            errorType = errorType,
            params = params,
        )
    }
}
