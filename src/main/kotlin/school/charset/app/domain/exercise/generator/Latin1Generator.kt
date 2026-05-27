package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

/**
 * Latin-1 (ISO 8859-1) step generator for the sandbox. Trivial 1:1
 * mapping between code points and bytes over `U+0000`..`U+00FF` - no
 * endianness, no multi-byte, no surrogates.
 *
 * Plain class (no `EncodingExerciseGenerator` implementation) - exercise
 * generation is not wired yet for Latin-1; only the sandbox needs the
 * `buildEncodeStepsFor` / `buildDecodeStepsFor` entry points.
 */
class Latin1Generator(
    private val codec: Codec,
) {
    fun buildEncodeStepsFor(codePoint: CodePoint, granularity: Granularity): List<Step> {
        // Delegate the actual encode to Codec; it enforces the
        // `cp <= 0xFF` invariant and throws `EncoderException` otherwise.
        val bytes = codec.encode(codePoint, Encoding.Latin1)
        val byte = bytes[0].toInt() and 0xFF
        val binary = byte.toString(2).padStart(8, '0')

        return when (granularity) {
            Granularity.Verbose -> listOf(
                Step.Binary(expected = binary, length = 8),
                Step.HexBytes(expected = listOf(byte)),
            )

            Granularity.Standard,
            Granularity.Compact,
            -> listOf(
                Step.HexBytes(expected = listOf(byte)),
            )
        }
    }

    fun buildDecodeStepsFor(bytes: ByteArray, granularity: Granularity): List<Step> {
        // Latin-1 decode is the identity on the single byte: the byte's
        // unsigned value IS the code point (mapping 0x00..0xFF →
        // U+0000..U+00FF). The caller has already validated the size
        // (exactly 1 byte) - we still assert here for documentation /
        // debuggability if something else ever calls this.
        require(bytes.size == 1) {
            "Latin-1 decode requires exactly 1 byte, got ${bytes.size}"
        }
        val byte = bytes[0].toInt() and 0xFF
        val binary = byte.toString(2).padStart(8, '0')

        return when (granularity) {
            Granularity.Verbose -> listOf(
                Step.Binary(expected = binary, length = 8),
                Step.CodePointEntry(expected = byte),
            )

            Granularity.Standard,
            Granularity.Compact,
            -> listOf(
                Step.CodePointEntry(expected = byte),
            )
        }
    }
}
