package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise

/**
 * Per-encoding generator. Each implementation handles a single encoding and
 * declares which one via [encoding].
 *
 * `ExerciseGenerator` registers all implementations as a `Set` and dispatches
 * incoming `generateEncode` / `generateDecode` calls to the matching implementation.
 * Adding a new encoding = create a new class implementing this interface and
 * register it as a `@Bean` - no edit to `ExerciseGenerator` needed.
 *
 * For encodings that come in two byte-order variants (UTF-16 BE/LE, UTF-32
 * BE/LE), the same class is parameterized at construction with the target
 * `Encoding` - two instances are registered.
 *
 * Both `generateEncode` and `generateDecode` are abstract: every per-encoding
 * generator must support both directions. This is enforced at compile time
 * rather than relying on a runtime `TODO()` default.
 */
interface EncodingExerciseGenerator {
    val encoding: Encoding

    fun generateEncode(level: Int): Exercise.Encode

    fun generateDecode(level: Int): Exercise.Decode
}
