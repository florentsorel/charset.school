package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf8Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
    private val byteArrayGenerator: ByteArrayGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Utf8

    override fun generateEncode(level: Int, granularity: Granularity): Exercise.Encode {
        val utf8Level = parseLevel(level)
        val codePoint = codePointGenerator.randomUtf8(utf8Level)
        val steps = codePoint.buildEncodeSteps(granularity)
        return Exercise.Encode(codePoint, Encoding.Utf8, level, granularity, steps)
    }

    fun buildEncodeStepsFor(codePoint: CodePoint, granularity: Granularity): List<Step> = codePoint.buildEncodeSteps(granularity)

    fun buildDecodeStepsFor(bytes: ByteArray, codePoint: CodePoint, granularity: Granularity): List<Step> = bytes.buildDecodeSteps(codePoint, granularity)

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val utf8Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf8(utf8Level)
        val codePoint = codec.decode(bytes, Encoding.Utf8)
        val steps = bytes.buildDecodeSteps(codePoint, granularity)
        return Exercise.Decode(bytes, codePoint, Encoding.Utf8, level, granularity, steps)
    }

    private fun parseLevel(level: Int): Utf8Level = Utf8Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = Encoding.Utf8,
            level = level,
            reason = "level must be one of: ${Utf8Level.validNumbers}",
        )

    private fun CodePoint.buildEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Utf8)
        val byteCount = bytes.size
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val byteCountLabel = FORMAT_CHOICES[byteCount - 1]
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
    ): List<Step> {
        val dataBits = dataBitsForByteCount(byteCount)
        val binary = value.toString(2).padStart(dataBits, '0')
        val binaryStep = Step.Binary(expected = binary, length = dataBits)

        return if (byteCount == 1) {
            // Single-byte UTF-8: the binary IS the byte (no marker bits to add,
            // no bit groups to split).
            listOf(formatStep, binaryStep, hexStep)
        } else {
            val bitGroupsStep = Step.BitGroups(expected = splitBits(binary, byteCount))
            listOf(formatStep, binaryStep, bitGroupsStep, hexStep)
        }
    }

    private fun ByteArray.buildDecodeSteps(codePoint: CodePoint, granularity: Granularity): List<Step> {
        // Build the pedagogical artefacts (bit groups, combined binary)
        // from an already-decoded code point. The caller decodes once via
        // `Codec.decode` and passes the result, so we don't double-decode
        // here. Conceptually this mirrors the encode flow (inspect leading
        // byte for byte count, strip markers per byte, combine into the
        // code point's binary), but the math was already done in Codec.
        val byteCount = size
        val codePointValue = codePoint.value
        val dataBits = dataBitsForByteCount(byteCount)
        val combinedBinary = codePointValue.toString(2).padStart(dataBits, '0')
        val byteCountLabel = FORMAT_CHOICES[byteCount - 1]
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel)
        val codePointStep = Step.CodePointEntry(expected = codePointValue)

        return when (granularity) {
            Granularity.Verbose -> verboseDecodeSteps(byteCount, combinedBinary, dataBits, formatStep, codePointStep)
            Granularity.Standard -> listOf(formatStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun verboseDecodeSteps(
        byteCount: Int,
        combinedBinary: String,
        dataBits: Int,
        formatStep: Step.Format,
        codePointStep: Step.CodePointEntry,
    ): List<Step> {
        val binaryStep = Step.Binary(expected = combinedBinary, length = dataBits)
        return if (byteCount == 1) {
            // Single-byte UTF-8: the data bits ARE the binary, no markers to strip.
            listOf(formatStep, binaryStep, codePointStep)
        } else {
            // Multi-byte: BitGroups shows the data bits per byte (after stripping
            // the 110/1110/11110 and 10 markers). Same shape as encode BitGroups.
            val bitGroupsStep = Step.BitGroups(expected = splitBits(combinedBinary, byteCount))
            listOf(formatStep, bitGroupsStep, binaryStep, codePointStep)
        }
    }

    private fun dataBitsForByteCount(byteCount: Int): Int = when (byteCount) {
        1 -> 7
        2 -> 11
        3 -> 16
        4 -> 21
        else -> error("Invalid UTF-8 byte count: $byteCount")
    }

    private fun splitBits(binary: String, byteCount: Int): List<String> = when (byteCount) {
        2 -> listOf(binary.substring(0, 5), binary.substring(5, 11))

        3 -> listOf(binary.substring(0, 4), binary.substring(4, 10), binary.substring(10, 16))

        4 -> listOf(
            binary.substring(0, 3),
            binary.substring(3, 9),
            binary.substring(9, 15),
            binary.substring(15, 21),
        )

        else -> error("BitGroups split not defined for byte count: $byteCount")
    }

    private companion object {
        // Indexed [0..3] = [ONE_BYTE..FOUR_BYTES] so `FORMAT_CHOICES[byteCount - 1]`
        // returns the matching label.
        private val FORMAT_CHOICES = listOf(
            FormatChoice.ONE_BYTE,
            FormatChoice.TWO_BYTES,
            FormatChoice.THREE_BYTES,
            FormatChoice.FOUR_BYTES,
        )
    }
}
