package school.charset.app.domain.exercise.generator

enum class Utf8Level(val number: Int) {
    OneByte(1), // U+0000..U+007F (= ASCII subset)
    TwoByte(2), // U+0080..U+07FF
    ThreeByte(3), // U+0800..U+FFFF (excluding surrogates)
    FourByte(4), // U+10000..U+10FFFF
    ;

    companion object {
        fun fromNumber(n: Int): Utf8Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
