package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.encoding.Windows1252Spec
import school.charset.app.domain.exercise.ExerciseGenerationException
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

    fun randomWindows1252(level: Int): CodePoint {
        val pool = when (level) {
            // The 27 special code points (€, Œ, ™, smart quotes, etc.) - the
            // range that distinguishes Windows-1252 from Latin-1. Forces the
            // user to engage with the Win-1252 special block.
            1 -> Windows1252Spec.specialCodePoints

            // All 251 encodable code points: ASCII identity + special block +
            // Latin-1 supplement identity. Uniform distribution over the whole
            // Windows-1252 space.
            2 -> Windows1252Spec.encodableCodePoints

            else -> throw ExerciseGenerationException(
                encoding = Encoding.Windows1252,
                level = level,
                reason = "level must be 1 or 2",
            )
        }
        return CodePoint(pool[random.nextInt(0, pool.size)])
    }
}
