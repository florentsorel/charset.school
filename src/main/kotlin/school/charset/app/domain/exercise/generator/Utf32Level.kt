package school.charset.app.domain.exercise.generator

enum class Utf32Level(val number: Int) {
    Bmp(1), // U+0000..U+FFFF excluding surrogates (bytes pattern [00, 00, ??, ??])
    Supplementary(2), // U+10000..U+10FFFF (bytes pattern [00, 0?, ??, ??])
    ;

    companion object {
        fun fromNumber(n: Int): Utf32Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
