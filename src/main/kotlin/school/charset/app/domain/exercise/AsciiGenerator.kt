package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding

class AsciiGenerator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Ascii

    override fun generate(level: Int, granularity: Granularity): Exercise {
        val codePoint = codePointGenerator.randomAscii(level)
        val steps = codePoint.buildSteps(granularity)
        return Exercise(codePoint, Encoding.Ascii, level, granularity, steps)
    }

    private fun CodePoint.buildSteps(granularity: Granularity): List<Step> {
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
}
