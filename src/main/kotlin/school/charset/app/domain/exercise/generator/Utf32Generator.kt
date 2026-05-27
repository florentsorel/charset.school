package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

/**
 * UTF-32 step generator for the sandbox. The simplest of the Unicode
 * transformation formats on paper: every code point takes exactly 4
 * bytes, with no variable-length encoding and no surrogate pairs. The
 * only nuance is endianness - the same 4 bytes can be laid out
 * high-order first (BE) or low-order first (LE).
 *
 * For now this is a plain class (no `EncodingExerciseGenerator`
 * implementation) - exercise generation will be wired in Phase 5.
 */
class Utf32Generator(
    private val codec: Codec,
) {
    fun buildEncodeStepsFor(
        codePoint: CodePoint,
        endian: Encoding.Endian,
        granularity: Granularity,
    ): List<Step> {
        // Re-use Codec for the actual encode work; derive the pedagogical
        // binary form by padding the code point's bits to a full 32-bit
        // width (the top 11 bits are always zero since Unicode caps at
        // U+10FFFF = 21 bits).
        val encoding = endian.toUtf32Encoding()
        val bytes = codec.encode(codePoint, encoding)
        val hexBytes = bytes.map { it.toInt() and 0xFF }

        val endianStep = Step.Endianness(expected = endian)
        val binaryStep = Step.Binary(
            expected = codePoint.value.toString(2).padStart(UTF32_BITS, '0'),
            length = UTF32_BITS,
        )
        val hexStep = Step.HexBytes(expected = hexBytes)

        return when (granularity) {
            Granularity.Verbose -> listOf(endianStep, binaryStep, hexStep)
            Granularity.Standard -> listOf(endianStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    fun buildDecodeStepsFor(
        bytes: ByteArray,
        codePoint: CodePoint,
        endian: Encoding.Endian,
        granularity: Granularity,
    ): List<Step> {
        // Derive the 32-bit binary from the user-provided bytes after
        // applying endianness: BE keeps the byte order as-is, LE flips it
        // so the high-order byte ends up first. This makes the Binary step
        // pedagogically faithful to what the decoder reads from the input
        // (instead of recomputing from the already-decoded code point) and
        // guarantees the displayed binary cannot drift from the actual
        // byte sequence.
        val orderedBytes = when (endian) {
            Encoding.Endian.BigEndian -> bytes
            Encoding.Endian.LittleEndian -> bytes.reversedArray()
        }
        val binary = orderedBytes.joinToString("") {
            (it.toInt() and 0xFF).toString(2).padStart(8, '0')
        }

        val endianStep = Step.Endianness(expected = endian)
        val binaryStep = Step.Binary(expected = binary, length = UTF32_BITS)
        val codePointStep = Step.CodePointEntry(expected = codePoint.value)

        return when (granularity) {
            Granularity.Verbose -> listOf(endianStep, binaryStep, codePointStep)
            Granularity.Standard -> listOf(endianStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun Encoding.Endian.toUtf32Encoding(): Encoding = when (this) {
        Encoding.Endian.BigEndian -> Encoding.Utf32Be
        Encoding.Endian.LittleEndian -> Encoding.Utf32Le
    }

    private companion object {
        // UTF-32 always uses 32 bits regardless of code point magnitude.
        // The top 11 bits are always zero (max valid code point is
        // U+10FFFF = 21 bits), which makes the padding pedagogically
        // visible: it shows the format's fixed width and where the
        // significant data lives.
        private const val UTF32_BITS = 32
    }
}
