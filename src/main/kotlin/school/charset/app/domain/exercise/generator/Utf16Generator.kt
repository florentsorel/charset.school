package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf16Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
    override val encoding: Encoding,
) : EncodingExerciseGenerator {
    init {
        require(encoding in SUPPORTED_ENCODINGS) {
            "Utf16Generator handles only ${SUPPORTED_ENCODINGS.joinToString { it.id }}, got ${encoding.id}"
        }
    }

    override fun generate(level: Int, granularity: Granularity): Exercise {
        val utf16Level = Utf16Level.fromNumber(level)
            ?: throw ExerciseGenerationException(
                encoding = encoding,
                level = level,
                reason = "level must be one of: ${Utf16Level.validNumbers}",
            )
        val codePoint = codePointGenerator.randomUtf16(utf16Level)
        val steps = codePoint.buildSteps(granularity)
        return Exercise(codePoint, encoding, level, granularity, steps)
    }

    private fun CodePoint.buildSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, encoding)
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val byteCount = bytes.size // 2 for BMP, 4 for surrogate pair
        val byteCountLabel = if (byteCount == 2) FormatChoice.TWO_BYTES else FormatChoice.FOUR_BYTES
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel)
        val hexStep = Step.HexBytes(expected = hexBytes)

        return when (granularity) {
            Granularity.Verbose -> verboseSteps(byteCount, formatStep, hexStep)
            Granularity.Standard -> listOf(formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    private fun CodePoint.verboseSteps(
        byteCount: Int,
        formatStep: Step.Format,
        hexStep: Step.HexBytes,
    ): List<Step> = if (byteCount == 2) {
        // BMP: code point fits in a single 16-bit code unit; the binary IS the
        // 2-byte value (no surrogate pair math).
        val binary = value.toString(2).padStart(16, '0')
        listOf(
            formatStep,
            Step.Binary(expected = binary, length = 16),
            hexStep,
        )
    } else {
        // Supplementary: subtract 0x10000, split the 20-bit offset into
        // high 10 / low 10 bits (each then OR'd with 0xD800 / 0xDC00 to form
        // the surrogate pair).
        val offset = value - 0x10000
        val binaryOffset = offset.toString(2).padStart(20, '0')
        val high10 = binaryOffset.substring(0, 10)
        val low10 = binaryOffset.substring(10, 20)
        listOf(
            formatStep,
            Step.Binary(expected = binaryOffset, length = 20),
            Step.BitGroups(expected = listOf(high10, low10)),
            hexStep,
        )
    }

    private companion object {
        private val FORMAT_CHOICES = listOf(FormatChoice.TWO_BYTES, FormatChoice.FOUR_BYTES)
        private val SUPPORTED_ENCODINGS = setOf(Encoding.Utf16Be, Encoding.Utf16Le)
    }
}
