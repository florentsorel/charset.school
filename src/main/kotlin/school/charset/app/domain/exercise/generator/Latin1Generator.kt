package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Step

class Latin1Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
    private val byteArrayGenerator: ByteArrayGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Latin1

    override fun generateEncode(level: Int): Exercise.Encode {
        val latin1Level = parseLevel(level)
        val codePoint = codePointGenerator.randomLatin1(latin1Level)
        val steps = buildEncodeStepsFor(codePoint)
        return Exercise.Encode(codePoint, Encoding.Latin1, level, steps)
    }

    override fun generateDecode(level: Int): Exercise.Decode {
        val latin1Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomLatin1(latin1Level)
        val codePoint = codec.decode(bytes, Encoding.Latin1)
        val steps = buildDecodeStepsFor(bytes)
        return Exercise.Decode(bytes, codePoint, Encoding.Latin1, level, steps)
    }

    fun buildEncodeStepsFor(codePoint: CodePoint): List<Step> {
        val bytes = codec.encode(codePoint, Encoding.Latin1)
        val byte = bytes[0].toInt() and 0xFF
        val binary = byte.toString(2).padStart(8, '0')
        return listOf(
            Step.Binary(expected = binary, length = 8),
            Step.HexBytes(expected = listOf(byte)),
        )
    }

    fun buildDecodeStepsFor(bytes: ByteArray): List<Step> {
        require(bytes.size == 1) {
            "Latin-1 decode requires exactly 1 byte, got ${bytes.size}"
        }
        val byte = bytes[0].toInt() and 0xFF
        val binary = byte.toString(2).padStart(8, '0')
        return listOf(
            Step.Binary(expected = binary, length = 8),
            Step.CodePointEntry(expected = byte),
        )
    }

    private fun parseLevel(level: Int): Latin1Level = Latin1Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = Encoding.Latin1,
            level = level,
            reason = "level must be one of: ${Latin1Level.validNumbers}",
        )
}
