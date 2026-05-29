package school.charset.app.domain.exercise.generator

enum class Utf8Level(val number: Int) {
    OneByte(1), // U+0000..U+007F (= ASCII subset)
    TwoByte(2), // U+0080..U+07FF
    ThreeByte(3), // U+0800..U+FFFF (excluding surrogates)
    FourByte(4), // U+10000..U+10FFFF
    ;

    // Weighted mix of byte-count sub-ranges to draw from at this tier.
    // Mixing prevents the Format step from being a trivial deterministic
    // answer (otherwise level N => N bytes always) and keeps prior byte
    // counts in rotation for spiral practice.
    val distribution: Map<Utf8Level, Int>
        get() = when (this) {
            OneByte -> mapOf(OneByte to 100)
            TwoByte -> mapOf(OneByte to 20, TwoByte to 80)
            ThreeByte -> mapOf(OneByte to 10, TwoByte to 25, ThreeByte to 65)
            FourByte -> mapOf(OneByte to 10, TwoByte to 30, ThreeByte to 30, FourByte to 30)
        }

    companion object {
        fun fromNumber(n: Int): Utf8Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
