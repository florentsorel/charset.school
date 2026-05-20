package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Latin1Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Latin1

    override fun generate(level: Int, granularity: Granularity): Exercise {
        val latin1Level = Latin1Level.fromNumber(level)
            ?: throw ExerciseGenerationException(
                encoding = Encoding.Latin1,
                level = level,
                reason = "level must be one of: ${Latin1Level.validNumbers}",
            )
        val codePoint = codePointGenerator.randomLatin1(latin1Level)
        val steps = codePoint.buildSteps(granularity)
        return Exercise(codePoint, Encoding.Latin1, level, granularity, steps)
    }

    private fun CodePoint.buildSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Latin1)
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
}
