package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding

/**
 * A single exercise instance, broken down into a sequence of steps according to
 * the requested granularity.
 *
 * Two directions, modelled as sealed subtypes:
 *
 * - `Exercise.Encode`: the user receives a code point and must produce its bytes
 *   in the target encoding.
 * - `Exercise.Decode`: the user receives bytes and must identify which code point
 *   they decode to.
 *
 * Both directions reuse the same `Step` sealed class for their step composition
 * - only the **input** shown to the user differs.
 *
 * Produced by `ExerciseGenerator`. Consumed by the front (which renders the
 * input + the step widgets) and by `AnswerValidator` (which checks each step
 * answer against `Step.expected`).
 */
sealed class Exercise {
    abstract val codePoint: CodePoint
    abstract val encoding: Encoding
    abstract val level: Int
    abstract val granularity: Granularity
    abstract val steps: List<Step>

    data class Encode(
        override val codePoint: CodePoint,
        override val encoding: Encoding,
        override val level: Int,
        override val granularity: Granularity,
        override val steps: List<Step>,
    ) : Exercise()

    data class Decode(
        val bytes: ByteArray,
        override val codePoint: CodePoint,
        override val encoding: Encoding,
        override val level: Int,
        override val granularity: Granularity,
        override val steps: List<Step>,
    ) : Exercise() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Decode) return false
            return bytes.contentEquals(other.bytes) &&
                codePoint == other.codePoint &&
                encoding == other.encoding &&
                level == other.level &&
                granularity == other.granularity &&
                steps == other.steps
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + codePoint.hashCode()
            result = 31 * result + encoding.hashCode()
            result = 31 * result + level.hashCode()
            result = 31 * result + granularity.hashCode()
            result = 31 * result + steps.hashCode()
            return result
        }
    }
}
