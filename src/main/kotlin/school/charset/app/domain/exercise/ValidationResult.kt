package school.charset.app.domain.exercise

/**
 * Result of validating a single (Step, Answer) pair.
 *
 * Anti-cheat note: this type intentionally does NOT carry the expected value.
 * Returning it to the front would let a user read the answer from the network
 * payload (devtools, etc.). The expected value lives only in the Step (server-side)
 * and is persisted directly via the repository.
 *
 * `params` may carry structural hints (length, count, position, ranges) but must
 * never contain the canonical expected value when `ok` is false.
 */
data class ValidationResult(
    val ok: Boolean,
    val errorType: String? = null,
    val params: Map<String, String> = emptyMap(),
) {
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
