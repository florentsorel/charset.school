package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf8Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Utf8

    override fun generate(level: Int, granularity: Granularity): Exercise {
        val utf8Level = Utf8Level.fromNumber(level)
            ?: throw ExerciseGenerationException(
                encoding = Encoding.Utf8,
                level = level,
                reason = "level must be one of: ${Utf8Level.validNumbers}",
            )
        val codePoint = codePointGenerator.randomUtf8(utf8Level)
        val steps = codePoint.buildSteps(granularity)
        return Exercise(codePoint, Encoding.Utf8, level, granularity, steps)
    }

    private fun CodePoint.buildSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Utf8)
        val byteCount = bytes.size
        val hexBytes = bytes.map { it.toInt() and 0xFF }
        val choices = FORMAT_CHOICES
        val byteCountLabel = choices[byteCount - 1]
        val formatStep = Step.Format(choices = choices, expected = byteCountLabel)
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
        private val FORMAT_CHOICES = listOf("1 byte", "2 bytes", "3 bytes", "4 bytes")
    }
}
