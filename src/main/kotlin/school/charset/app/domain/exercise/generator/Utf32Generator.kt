package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf32Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
    override val encoding: Encoding,
) : EncodingExerciseGenerator {
    init {
        require(encoding in SUPPORTED_ENCODINGS) {
            "Utf32Generator handles only ${SUPPORTED_ENCODINGS.joinToString { it.id }}, got ${encoding.id}"
        }
    }

    override fun generate(level: Int, granularity: Granularity): Exercise {
        val utf32Level = Utf32Level.fromNumber(level)
            ?: throw ExerciseGenerationException(
                encoding = encoding,
                level = level,
                reason = "level must be one of: ${Utf32Level.validNumbers}",
            )
        val codePoint = codePointGenerator.randomUtf32(utf32Level)
        val steps = codePoint.buildSteps(granularity)
        return Exercise(codePoint, encoding, level, granularity, steps)
    }

    private fun CodePoint.buildSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, encoding)
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = FormatChoice.FOUR_BYTES)
        val hexStep = Step.HexBytes(expected = hexBytes)

        return when (granularity) {
            Granularity.Verbose -> verboseSteps(formatStep, hexStep)
            Granularity.Standard -> listOf(formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    private fun CodePoint.verboseSteps(
        formatStep: Step.Format,
        hexStep: Step.HexBytes,
    ): List<Step> {
        // The code point fits in 21 bits but UTF-32 always uses 32 bits — pad
        // explicitly so the BitGroups split into 4×8 is mechanical.
        val binary = value.toString(2).padStart(32, '0')
        val bitGroups = listOf(
            binary.substring(0, 8),
            binary.substring(8, 16),
            binary.substring(16, 24),
            binary.substring(24, 32),
        )
        return listOf(
            formatStep,
            Step.Binary(expected = binary, length = 32),
            Step.BitGroups(expected = bitGroups),
            hexStep,
        )
    }

    private companion object {
        // Same 4 byte-count choices as UTF-8 — for UTF-32 the right answer is always
        // FOUR_BYTES; the other options are pedagogical decoys (force the user to
        // actively confirm that UTF-32 is fixed-width).
        private val FORMAT_CHOICES = listOf(
            FormatChoice.ONE_BYTE,
            FormatChoice.TWO_BYTES,
            FormatChoice.THREE_BYTES,
            FormatChoice.FOUR_BYTES,
        )
        private val SUPPORTED_ENCODINGS = setOf(Encoding.Utf32Be, Encoding.Utf32Le)
    }
}
