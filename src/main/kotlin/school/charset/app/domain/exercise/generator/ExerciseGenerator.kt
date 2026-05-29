package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException

/**
 * Thin dispatcher: routes `generateEncode` / `generateDecode` calls to the
 * matching per-encoding generator. Built from a `Set<EncodingExerciseGenerator>`
 * collected at wiring time (via Spring `@Bean` auto-wiring of the Set).
 *
 * Adding a new encoding means: implement `EncodingExerciseGenerator` and register
 * the new class as a `@Bean`. No edit to this class is ever needed.
 */
class ExerciseGenerator(
    generators: Set<EncodingExerciseGenerator>,
) {
    private val byEncoding: Map<Encoding, EncodingExerciseGenerator> =
        generators.associateBy { it.encoding }

    init {
        require(generators.size == byEncoding.size) {
            "Duplicate generators registered for the same encoding"
        }
    }

    fun generateEncode(encoding: Encoding, level: Int): Exercise.Encode {
        val generator = byEncoding[encoding]
            ?: throw ExerciseGenerationException(
                encoding = encoding,
                level = level,
                reason = "no generator registered for this encoding",
            )
        return generator.generateEncode(level)
    }

    fun generateDecode(encoding: Encoding, level: Int): Exercise.Decode {
        val generator = byEncoding[encoding]
            ?: throw ExerciseGenerationException(
                encoding = encoding,
                level = level,
                reason = "no generator registered for this encoding",
            )
        return generator.generateDecode(level)
    }
}
