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
    private val byteArrayGenerator: ByteArrayGenerator,
    override val encoding: Encoding,
) : EncodingExerciseGenerator {
    init {
        require(encoding in SUPPORTED_ENCODINGS) {
            "Utf32Generator handles only ${SUPPORTED_ENCODINGS.joinToString { it.id }}, got ${encoding.id}"
        }
    }

    override fun generateEncode(level: Int, granularity: Granularity): Exercise.Encode {
        val utf32Level = parseLevel(level)
        val codePoint = codePointGenerator.randomUtf32(utf32Level)
        val steps = codePoint.buildEncodeSteps(granularity)
        return Exercise.Encode(codePoint, encoding, level, granularity, steps)
    }

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val utf32Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf32(utf32Level, encoding)
        val steps = bytes.buildDecodeSteps(granularity)
        return Exercise.Decode(bytes, encoding, level, granularity, steps)
    }

    private fun parseLevel(level: Int): Utf32Level = Utf32Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = encoding,
            level = level,
            reason = "level must be one of: ${Utf32Level.validNumbers}",
        )

    private fun CodePoint.buildEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, encoding)
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = FormatChoice.FOUR_BYTES)
        val hexStep = Step.HexBytes(expected = hexBytes)

        return when (granularity) {
            Granularity.Verbose -> verboseEncodeSteps(formatStep, hexStep)
            Granularity.Standard -> listOf(formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    private fun CodePoint.verboseEncodeSteps(
        formatStep: Step.Format,
        hexStep: Step.HexBytes,
    ): List<Step> {
        // The code point fits in 21 bits but UTF-32 always uses 32 bits - pad
        // explicitly so the BitGroups split into 4×8 is mechanical.
        val binary = value.toString(2).padStart(32, '0')
        val bitGroups = splitIntoBytes(binary)
        return listOf(
            formatStep,
            Step.Binary(expected = binary, length = 32),
            Step.BitGroups(expected = bitGroups),
            hexStep,
        )
    }

    private fun ByteArray.buildDecodeSteps(granularity: Granularity): List<Step> {
        // Decode flow: combine the 4 bytes (per endianness) into a 32-bit value
        // - that value IS the code point.
        val codePoint = codec.decode(this, encoding).value
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = FormatChoice.FOUR_BYTES)
        val codePointStep = Step.CodePointEntry(expected = codePoint)

        return when (granularity) {
            Granularity.Verbose -> verboseDecodeSteps(codePoint, formatStep, codePointStep)
            Granularity.Standard -> listOf(formatStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun verboseDecodeSteps(
        codePoint: Int,
        formatStep: Step.Format,
        codePointStep: Step.CodePointEntry,
    ): List<Step> {
        val binary = codePoint.toString(2).padStart(32, '0')
        val bitGroups = splitIntoBytes(binary)
        return listOf(
            formatStep,
            Step.BitGroups(expected = bitGroups),
            Step.Binary(expected = binary, length = 32),
            codePointStep,
        )
    }

    private fun splitIntoBytes(binary32: String): List<String> = listOf(
        binary32.substring(0, 8),
        binary32.substring(8, 16),
        binary32.substring(16, 24),
        binary32.substring(24, 32),
    )

    private companion object {
        // Same 4 byte-count choices as UTF-8 - for UTF-32 the right answer is always
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
