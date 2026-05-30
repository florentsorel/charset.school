package school.charset.app.domain.exercise.generator

enum class Utf32Level(val number: Int) {
    Bmp(1), // U+0000..U+FFFF excluding surrogates (bytes pattern [00, 00, ??, ??])
    Supplementary(2), // U+10000..U+10FFFF (bytes pattern [00, 0?, ??, ??])
    ;

    val distribution: Map<Utf32Level, Int>
        get() = when (this) {
            Bmp -> mapOf(Bmp to 100)
            Supplementary -> mapOf(Bmp to 30, Supplementary to 70)
        }

    companion object {
        fun fromNumber(n: Int): Utf32Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
