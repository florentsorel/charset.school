package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class AsciiGenerator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
    private val byteArrayGenerator: ByteArrayGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Ascii

    override fun generateEncode(level: Int, granularity: Granularity): Exercise.Encode {
        val asciiLevel = parseLevel(level)
        val codePoint = codePointGenerator.randomAscii(asciiLevel)
        val steps = codePoint.buildEncodeSteps(granularity)
        return Exercise.Encode(codePoint, Encoding.Ascii, level, granularity, steps)
    }

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val asciiLevel = parseLevel(level)
        val bytes = byteArrayGenerator.randomAscii(asciiLevel)
        val steps = bytes.buildDecodeSteps(granularity)
        return Exercise.Decode(bytes, Encoding.Ascii, level, granularity, steps)
    }

    private fun parseLevel(level: Int): AsciiLevel = AsciiLevel.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = Encoding.Ascii,
            level = level,
            reason = "level must be one of: ${AsciiLevel.validNumbers}",
        )

    private fun CodePoint.buildEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Ascii)
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

    private fun ByteArray.buildDecodeSteps(granularity: Granularity): List<Step> {
        // ASCII decode is identity on the single byte: the byte's unsigned value
        // IS the code point (and its binary IS the byte's bit pattern).
        val byte = this[0].toInt() and 0xFF
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
