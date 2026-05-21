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
    private val byteArrayGenerator: ByteArrayGenerator,
    override val encoding: Encoding,
) : EncodingExerciseGenerator {
    init {
        require(encoding in SUPPORTED_ENCODINGS) {
            "Utf16Generator handles only ${SUPPORTED_ENCODINGS.joinToString { it.id }}, got ${encoding.id}"
        }
    }

    override fun generateEncode(level: Int, granularity: Granularity): Exercise.Encode {
        val utf16Level = parseLevel(level)
        val codePoint = codePointGenerator.randomUtf16(utf16Level)
        val steps = codePoint.buildEncodeSteps(granularity)
        return Exercise.Encode(codePoint, encoding, level, granularity, steps)
    }

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val utf16Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf16(utf16Level, encoding)
        val steps = bytes.buildDecodeSteps(granularity)
        return Exercise.Decode(bytes, encoding, level, granularity, steps)
    }

    private fun parseLevel(level: Int): Utf16Level = Utf16Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = encoding,
            level = level,
            reason = "level must be one of: ${Utf16Level.validNumbers}",
        )

    private fun CodePoint.buildEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, encoding)
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val byteCount = bytes.size // 2 for BMP, 4 for surrogate pair
        val byteCountLabel = if (byteCount == 2) FormatChoice.TWO_BYTES else FormatChoice.FOUR_BYTES
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel)
        val hexStep = Step.HexBytes(expected = hexBytes)

        return when (granularity) {
            Granularity.Verbose -> verboseEncodeSteps(byteCount, formatStep, hexStep)
            Granularity.Standard -> listOf(formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    private fun CodePoint.verboseEncodeSteps(
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

    private fun ByteArray.buildDecodeSteps(granularity: Granularity): List<Step> {
        // Decode flow: identify byte count from size (2 = BMP, 4 = surrogate pair),
        // then either read the 2-byte code unit directly (BMP) or split surrogates
        // into high/low 10-bit groups and reconstruct the offset (supplementary).
        val byteCount = size
        val codePoint = codec.decode(this, encoding).value
        val byteCountLabel = if (byteCount == 2) FormatChoice.TWO_BYTES else FormatChoice.FOUR_BYTES
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel)
        val codePointStep = Step.CodePointEntry(expected = codePoint)

        return when (granularity) {
            Granularity.Verbose -> verboseDecodeSteps(byteCount, codePoint, formatStep, codePointStep)
            Granularity.Standard -> listOf(formatStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun verboseDecodeSteps(
        byteCount: Int,
        codePoint: Int,
        formatStep: Step.Format,
        codePointStep: Step.CodePointEntry,
    ): List<Step> = if (byteCount == 2) {
        // BMP: the binary representation of the code point in 16 bits IS the
        // combined 2-byte value (assembled from the input bytes regardless of
        // byte order).
        val binary = codePoint.toString(2).padStart(16, '0')
        listOf(
            formatStep,
            Step.Binary(expected = binary, length = 16),
            codePointStep,
        )
    } else {
        // Supplementary: the user splits the surrogate pair into high10/low10
        // data bits, combines into a 20-bit offset, then adds 0x10000 to get
        // the code point.
        val offset = codePoint - 0x10000
        val binaryOffset = offset.toString(2).padStart(20, '0')
        val high10 = binaryOffset.substring(0, 10)
        val low10 = binaryOffset.substring(10, 20)
        listOf(
            formatStep,
            Step.BitGroups(expected = listOf(high10, low10)),
            Step.Binary(expected = binaryOffset, length = 20),
            codePointStep,
        )
    }

    private companion object {
        private val FORMAT_CHOICES = listOf(FormatChoice.TWO_BYTES, FormatChoice.FOUR_BYTES)
        private val SUPPORTED_ENCODINGS = setOf(Encoding.Utf16Be, Encoding.Utf16Le)
    }
}
