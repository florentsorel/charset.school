package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import kotlin.random.Random

class CodePointGenerator(
    private val random: Random,
) {
    fun randomAscii(level: Int): CodePoint {
        val range = when (level) {
            // Printable ASCII: U+0020..U+007E (excludes C0 controls U+0000..U+001F
            // and DEL U+007F).
            1 -> 0x20..0x7E

            // Full ASCII: U+0000..U+007F (includes C0 controls and DEL).
            2 -> 0x00..0x7F

            else -> throw ExerciseGenerationException(
                encoding = Encoding.Ascii,
                level = level,
                reason = "level must be 1 or 2",
            )
        }
        return CodePoint(random.nextInt(range.first, range.last + 1))
    }

    fun randomLatin1(level: Int): CodePoint {
        val range = when (level) {
            // Printable Latin-1 supplement: U+00A0..U+00FF (excludes C1 controls
            // U+0080..U+009F). Accented letters and symbols — the range that
            // distinguishes Latin-1 from ASCII in practice.
            1 -> 0xA0..0xFF

            // Full Latin-1: U+0000..U+00FF (includes ASCII subset and both C0
            // U+0000..U+001F and C1 U+0080..U+009F control chars).
            2 -> 0x00..0xFF

            else -> throw ExerciseGenerationException(
                encoding = Encoding.Latin1,
                level = level,
                reason = "level must be 1 or 2",
            )
        }
        return CodePoint(random.nextInt(range.first, range.last + 1))
    }
}
