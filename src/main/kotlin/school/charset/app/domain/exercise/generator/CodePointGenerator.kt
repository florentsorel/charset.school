package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Windows1252Spec
import kotlin.random.Random

class CodePointGenerator(
    private val random: Random,
) {
    fun randomAscii(level: AsciiLevel): CodePoint = CodePoint(asciiSample(weightedPick(level.distribution)))

    fun randomLatin1(level: Latin1Level): CodePoint = CodePoint(latin1Sample(weightedPick(level.distribution)))

    fun randomWindows1252(level: Windows1252Level): CodePoint = CodePoint(windows1252Sample(weightedPick(level.distribution)))

    fun randomUtf16(level: Utf16Level): CodePoint = CodePoint(utf16Sample(weightedPick(level.distribution)))

    fun randomUtf32(level: Utf32Level): CodePoint = CodePoint(utf32Sample(weightedPick(level.distribution)))

    fun randomUtf8(level: Utf8Level): CodePoint = CodePoint(utf8Sample(weightedPick(level.distribution)))

    // Weighted random pick across an enum's distribution map. The picked
    // entry is the sub-range to actually sample from at this tier; mixing
    // (e.g. level 4 UTF-8 = 10/30/30/30) keeps lower byte-counts in
    // rotation for spiral practice and prevents the Format step from
    // being a trivial deterministic answer.
    private fun <L> weightedPick(distribution: Map<L, Int>): L {
        // Single-entry distributions (level 1 = 100% of its sub-range) skip
        // the Random call entirely — keeps existing boundary tests stable
        // and saves an allocation/RNG draw on the hot path.
        if (distribution.size == 1) return distribution.keys.first()
        val total = distribution.values.sum()
        require(total > 0) { "Total weight must be positive, got $total for $distribution" }
        var roll = random.nextInt(0, total)
        for ((subLevel, weight) in distribution) {
            roll -= weight
            if (roll < 0) return subLevel
        }
        error("Weighted pick fell through (rounding bug)")
    }

    private fun asciiSample(sub: AsciiLevel): Int = when (sub) {
        // Printable ASCII: U+0020..U+007E (excludes C0 controls and DEL).
        AsciiLevel.Printable -> random.nextInt(0x20, 0x7F)

        // Full ASCII: U+0000..U+007F.
        AsciiLevel.Full -> random.nextInt(0x00, 0x80)
    }

    private fun latin1Sample(sub: Latin1Level): Int = when (sub) {
        // Printable Latin-1 supplement: U+00A0..U+00FF.
        Latin1Level.Supplement -> random.nextInt(0xA0, 0x100)

        // Full Latin-1: U+0000..U+00FF (includes ASCII subset, C0 and C1 controls).
        Latin1Level.Full -> random.nextInt(0x00, 0x100)
    }

    private fun windows1252Sample(sub: Windows1252Level): Int = when (sub) {
        // The 27 special code points (€, Œ, ™, smart quotes, ...).
        Windows1252Level.SpecialBlock -> Windows1252Spec.specialCodePoints.let {
            it[random.nextInt(0, it.size)]
        }

        // All 251 encodable code points: ASCII identity + special block + Latin-1 supplement identity.
        Windows1252Level.AllEncodable -> Windows1252Spec.encodableCodePoints.let {
            it[random.nextInt(0, it.size)]
        }
    }

    private fun utf16Sample(sub: Utf16Level): Int = when (sub) {
        Utf16Level.Bmp -> nonSurrogateBmp()
        Utf16Level.Supplementary -> random.nextInt(0x10000, 0x110000)
    }

    private fun utf32Sample(sub: Utf32Level): Int = when (sub) {
        Utf32Level.Bmp -> nonSurrogateBmp()
        Utf32Level.Supplementary -> random.nextInt(0x10000, 0x110000)
    }

    private fun utf8Sample(sub: Utf8Level): Int = when (sub) {
        // 1-byte UTF-8: U+0000..U+007F (= ASCII subset, 7 data bits).
        Utf8Level.OneByte -> random.nextInt(0x00, 0x80)

        // 2-byte UTF-8: U+0080..U+07FF (Latin extensions, Greek, Cyrillic; 11 data bits).
        Utf8Level.TwoByte -> random.nextInt(0x80, 0x800)

        // 3-byte UTF-8: U+0800..U+FFFF excluding surrogates U+D800..U+DFFF.
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

    /**
     * Picks a non-surrogate BMP code point: U+0000..U+FFFF excluding U+D800..U+DFFF.
     * Shared between UTF-16 BMP level and UTF-32 BMP level - same range, same skip.
     */
    private fun nonSurrogateBmp(): Int {
        val index = random.nextInt(0, NON_SURROGATE_BMP_COUNT)
        return if (index < CodePoint.SURROGATE_MIN) {
            index
        } else {
            AFTER_SURROGATES_START + (index - CodePoint.SURROGATE_MIN)
        }
    }

    private companion object {
        private const val THREE_BYTE_START = 0x800
        private const val AFTER_SURROGATES_START = CodePoint.SURROGATE_MAX + 1
        private const val THREE_BYTE_END_EXCLUSIVE = CodePoint.BMP_MAX + 1

        private const val BEFORE_SURROGATES_COUNT = CodePoint.SURROGATE_MIN - THREE_BYTE_START

        private const val NON_SURROGATE_3_BYTE_COUNT =
            (THREE_BYTE_END_EXCLUSIVE - THREE_BYTE_START) -
                (AFTER_SURROGATES_START - CodePoint.SURROGATE_MIN)

        private const val NON_SURROGATE_BMP_COUNT =
            (CodePoint.BMP_MAX + 1) -
                (CodePoint.SURROGATE_MAX - CodePoint.SURROGATE_MIN + 1)
    }
}
