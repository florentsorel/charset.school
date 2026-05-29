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
        val steps = codePoint.buildExerciseEncodeSteps(granularity)
        return Exercise.Encode(codePoint, Encoding.Utf8, level, granularity, steps)
    }

    // Sandbox uses the historical layout: useful-bit binary (no padding) and no
    // UsefulBitCount step, since the sandbox visualises the encoding rather
    // than asking the user pedagogical questions.
    fun buildEncodeStepsFor(codePoint: CodePoint, granularity: Granularity): List<Step> = codePoint.buildSandboxEncodeSteps(granularity)

    fun buildDecodeStepsFor(bytes: ByteArray, codePoint: CodePoint, granularity: Granularity): List<Step> = bytes.buildSandboxDecodeSteps(codePoint, granularity)

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val utf8Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf8(utf8Level)
        val codePoint = codec.decode(bytes, Encoding.Utf8)
        val steps = bytes.buildExerciseDecodeSteps(codePoint, granularity)
        return Exercise.Decode(bytes, codePoint, Encoding.Utf8, level, granularity, steps)
    }

    private fun parseLevel(level: Int): Utf8Level = Utf8Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = Encoding.Utf8,
            level = level,
            reason = "level must be one of: ${Utf8Level.validNumbers}",
        )

    // EXERCISE flow: byte-aligned padded binary + explicit UsefulBitCount step,
    // so the user explicitly thinks "I padded to a byte multiple, only N bits
    // are useful, split them into MSB/LSB packets per UTF-8 byte".
    private fun CodePoint.buildExerciseEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Utf8)
        val byteCount = bytes.size
        val (formatStep, hexStep) = formatAndHexFor(byteCount, bytes)

        return when (granularity) {
            Granularity.Verbose -> exerciseVerboseEncodeSteps(byteCount, formatStep, hexStep)
            Granularity.Standard -> listOf(formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    private fun CodePoint.exerciseVerboseEncodeSteps(
        byteCount: Int,
        formatStep: Step.Format,
        hexStep: Step.HexBytes,
    ): List<Step> {
        // ASCII range (1 byte): binary IS the byte, hex IS the code point.
        // Format step alone teaches the identity-range insight.
        if (byteCount == 1) return listOf(formatStep, hexStep)

        val dataBits = dataBitsForByteCount(byteCount)
        val paddedBits = paddedBitCount(dataBits)
        val paddedBinary = value.toString(2).padStart(paddedBits, '0')
        val usefulBits = paddedBinary.substring(paddedBits - dataBits)
        return listOf(
            formatStep,
            Step.Binary(expected = paddedBinary, length = paddedBits),
            Step.UsefulBitCount(expected = dataBits),
            Step.BitGroups(expected = splitBits(usefulBits, byteCount)),
            hexStep,
        )
    }

    private fun ByteArray.buildExerciseDecodeSteps(codePoint: CodePoint, granularity: Granularity): List<Step> {
        val byteCount = size
        val codePointValue = codePoint.value
        val dataBits = dataBitsForByteCount(byteCount)
        val combinedBinary = codePointValue.toString(2).padStart(dataBits, '0')
        val byteCountLabel = FORMAT_CHOICES[byteCount - 1]
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel)
        val codePointStep = Step.CodePointEntry(expected = codePointValue)

        return when (granularity) {
            Granularity.Verbose -> exerciseVerboseDecodeSteps(byteCount, combinedBinary, dataBits, formatStep, codePointStep)
            Granularity.Standard -> listOf(formatStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun exerciseVerboseDecodeSteps(
        byteCount: Int,
        combinedBinary: String,
        dataBits: Int,
        formatStep: Step.Format,
        codePointStep: Step.CodePointEntry,
    ): List<Step> {
        if (byteCount == 1) return listOf(formatStep, codePointStep)

        val paddedBits = paddedBitCount(dataBits)
        val paddedBinary = combinedBinary.padStart(paddedBits, '0')
        return listOf(
            formatStep,
            Step.BitGroups(expected = splitBits(combinedBinary, byteCount)),
            Step.UsefulBitCount(expected = dataBits),
            Step.Binary(expected = paddedBinary, length = paddedBits),
            codePointStep,
        )
    }

    // SANDBOX flow: legacy layout, no UsefulBitCount, useful-bit binary
    // (not byte-padded). The sandbox visualises the encoding mechanics rather
    // than asking interactive questions, so byte-alignment / explicit useful
    // count aren't needed.
    private fun CodePoint.buildSandboxEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Utf8)
        val byteCount = bytes.size
        val (formatStep, hexStep) = formatAndHexFor(byteCount, bytes)

        return when (granularity) {
            Granularity.Verbose -> sandboxVerboseEncodeSteps(byteCount, formatStep, hexStep)
            Granularity.Standard -> listOf(formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    private fun CodePoint.sandboxVerboseEncodeSteps(
        byteCount: Int,
        formatStep: Step.Format,
        hexStep: Step.HexBytes,
    ): List<Step> {
        val dataBits = dataBitsForByteCount(byteCount)
        val binary = value.toString(2).padStart(dataBits, '0')
        val binaryStep = Step.Binary(expected = binary, length = dataBits)

        return if (byteCount == 1) {
            listOf(formatStep, binaryStep, hexStep)
        } else {
            val bitGroupsStep = Step.BitGroups(expected = splitBits(binary, byteCount))
            listOf(formatStep, binaryStep, bitGroupsStep, hexStep)
        }
    }

    private fun ByteArray.buildSandboxDecodeSteps(codePoint: CodePoint, granularity: Granularity): List<Step> {
        val byteCount = size
        val codePointValue = codePoint.value
        val dataBits = dataBitsForByteCount(byteCount)
        val combinedBinary = codePointValue.toString(2).padStart(dataBits, '0')
        val byteCountLabel = FORMAT_CHOICES[byteCount - 1]
        val formatStep = Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel)
        val codePointStep = Step.CodePointEntry(expected = codePointValue)

        return when (granularity) {
            Granularity.Verbose -> sandboxVerboseDecodeSteps(byteCount, combinedBinary, dataBits, formatStep, codePointStep)
            Granularity.Standard -> listOf(formatStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun sandboxVerboseDecodeSteps(
        byteCount: Int,
        combinedBinary: String,
        dataBits: Int,
        formatStep: Step.Format,
        codePointStep: Step.CodePointEntry,
    ): List<Step> {
        val binaryStep = Step.Binary(expected = combinedBinary, length = dataBits)
        return if (byteCount == 1) {
            listOf(formatStep, binaryStep, codePointStep)
        } else {
            val bitGroupsStep = Step.BitGroups(expected = splitBits(combinedBinary, byteCount))
            listOf(formatStep, bitGroupsStep, binaryStep, codePointStep)
        }
    }

    private fun formatAndHexFor(byteCount: Int, bytes: ByteArray): Pair<Step.Format, Step.HexBytes> {
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val byteCountLabel = FORMAT_CHOICES[byteCount - 1]
        return Step.Format(choices = FORMAT_CHOICES, expected = byteCountLabel) to Step.HexBytes(expected = hexBytes)
    }

    private fun paddedBitCount(dataBits: Int): Int = ((dataBits + 7) / 8) * 8

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
