package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import kotlin.random.Random

class CodePointGenerator(
    private val random: Random,
) {
    fun randomAscii(level: Int): CodePoint {
        val range = when (level) {
            // printable ASCII (no control chars)
            1 -> 0x20..0x7E

            // full ASCII including control chars
            2 -> 0x00..0x7F

            else -> throw ExerciseGenerationException(
                encoding = Encoding.Ascii,
                level = level,
                reason = "level must be 1 or 2",
            )
        }
        return CodePoint(random.nextInt(range.first, range.last + 1))
    }
}
