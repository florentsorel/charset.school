package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Windows1252Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Windows1252

    override fun generate(level: Int, granularity: Granularity): Exercise {
        val windows1252Level = Windows1252Level.fromNumber(level)
            ?: throw ExerciseGenerationException(
                encoding = Encoding.Windows1252,
                level = level,
                reason = "level must be one of: ${Windows1252Level.validNumbers}",
            )
        val codePoint = codePointGenerator.randomWindows1252(windows1252Level)
        val steps = codePoint.buildSteps(granularity)
        return Exercise(codePoint, Encoding.Windows1252, level, granularity, steps)
    }

    private fun CodePoint.buildSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Windows1252)
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
