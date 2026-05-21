package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Windows1252Spec
import kotlin.random.Random

class CodePointGenerator(
    private val random: Random,
) {
    fun randomAscii(level: AsciiLevel): CodePoint {
        val range = when (level) {
            // Printable ASCII: U+0020..U+007E (excludes C0 controls U+0000..U+001F
            // and DEL U+007F).
            AsciiLevel.Printable -> 0x20..0x7E

            // Full ASCII: U+0000..U+007F (includes C0 controls and DEL).
            AsciiLevel.Full -> 0x00..0x7F
        }
        return CodePoint(random.nextInt(range.first, range.last + 1))
    }

    fun randomLatin1(level: Latin1Level): CodePoint {
        val range = when (level) {
            // Printable Latin-1 supplement: U+00A0..U+00FF (excludes C1 controls
            // U+0080..U+009F). Accented letters and symbols - the range that
            // distinguishes Latin-1 from ASCII in practice.
            Latin1Level.Supplement -> 0xA0..0xFF

            // Full Latin-1: U+0000..U+00FF (includes ASCII subset and both C0
            // U+0000..U+001F and C1 U+0080..U+009F control chars).
            Latin1Level.Full -> 0x00..0xFF
        }
        return CodePoint(random.nextInt(range.first, range.last + 1))
    }

    fun randomWindows1252(level: Windows1252Level): CodePoint {
        val pool = when (level) {
            // The 27 special code points (€, Œ, ™, smart quotes, etc.) - the
            // range that distinguishes Windows-1252 from Latin-1. Forces the
            // user to engage with the Win-1252 special block.
            Windows1252Level.SpecialBlock -> Windows1252Spec.specialCodePoints

            // All 251 encodable code points: ASCII identity + special block +
            // Latin-1 supplement identity. Uniform distribution over the whole
            // Windows-1252 space.
            Windows1252Level.AllEncodable -> Windows1252Spec.encodableCodePoints
        }
        return CodePoint(pool[random.nextInt(0, pool.size)])
    }

    fun randomUtf16(level: Utf16Level): CodePoint = when (level) {
        Utf16Level.Bmp -> randomBmpCodePoint()
        Utf16Level.Supplementary -> randomSupplementaryCodePoint()
    }

    fun randomUtf32(level: Utf32Level): CodePoint = when (level) {
        Utf32Level.Bmp -> randomBmpCodePoint()
        Utf32Level.Supplementary -> randomSupplementaryCodePoint()
    }

    fun randomUtf8(level: Utf8Level): CodePoint {
        val value = when (level) {
            // 1-byte UTF-8: U+0000..U+007F (= ASCII subset, 7 data bits).
            Utf8Level.OneByte -> random.nextInt(0x00, 0x80)

            // 2-byte UTF-8: U+0080..U+07FF (Latin extensions, Greek, Cyrillic; 11 data bits).
            Utf8Level.TwoByte -> random.nextInt(0x80, 0x800)

            // 3-byte UTF-8: U+0800..U+FFFF excluding surrogates U+D800..U+DFFF
            // (CJK, most BMP; 16 data bits). 63488 - 2048 surrogates = 61440 valid.
            Utf8Level.ThreeByte -> {
                val index = random.nextInt(0, NON_SURROGATE_3_BYTE_COUNT)
                if (index < BEFORE_SURROGATES_COUNT) {
                    THREE_BYTE_START + index
                } else {
                    AFTER_SURROGATES_START + (index - BEFORE_SURROGATES_COUNT)
                }
            }

            // 4-byte UTF-8: U+10000..U+10FFFF (supplementary plane; 21 data bits).
            Utf8Level.FourByte -> random.nextInt(0x10000, 0x110000)
        }
        return CodePoint(value)
    }

    /**
     * Picks a non-surrogate BMP code point: U+0000..U+FFFF excluding U+D800..U+DFFF.
     * Shared between UTF-16 BMP level and UTF-32 BMP level - same range, same skip.
     */
    private fun randomBmpCodePoint(): CodePoint {
        val index = random.nextInt(0, NON_SURROGATE_BMP_COUNT)
        val value = if (index < CodePoint.SURROGATE_MIN) {
            index
        } else {
            AFTER_SURROGATES_START + (index - CodePoint.SURROGATE_MIN)
        }
        return CodePoint(value)
    }

    /**
     * Picks a supplementary-plane code point: U+10000..U+10FFFF.
     * Shared between UTF-16 Supplementary level and UTF-32 Supplementary level.
     */
    private fun randomSupplementaryCodePoint(): CodePoint = CodePoint(random.nextInt(0x10000, 0x110000))

    private companion object {
        // UTF-8 level 3 covers U+0800..U+FFFF (= BMP starting at 3-byte boundary)
        // excluding the surrogate gap U+D800..U+DFFF. Constants are derived from
        // `CodePoint` so the surrogate range has a single source of truth.
        private const val THREE_BYTE_START = 0x800
        private const val AFTER_SURROGATES_START = CodePoint.SURROGATE_MAX + 1
        private const val THREE_BYTE_END_EXCLUSIVE = CodePoint.BMP_MAX + 1

        // Number of valid code points BEFORE the surrogate gap in the 3-byte range.
        private const val BEFORE_SURROGATES_COUNT = CodePoint.SURROGATE_MIN - THREE_BYTE_START

        // Total count of valid (non-surrogate) code points in the 3-byte range.
        private const val NON_SURROGATE_3_BYTE_COUNT =
            (THREE_BYTE_END_EXCLUSIVE - THREE_BYTE_START) -
                (AFTER_SURROGATES_START - CodePoint.SURROGATE_MIN)

        // BMP range count excluding surrogates, used for UTF-16 BMP level.
        // 65536 (full BMP) - 2048 (surrogates) = 63488 valid code points.
        private const val NON_SURROGATE_BMP_COUNT =
            (CodePoint.BMP_MAX + 1) -
                (CodePoint.SURROGATE_MAX - CodePoint.SURROGATE_MIN + 1)
    }
}
