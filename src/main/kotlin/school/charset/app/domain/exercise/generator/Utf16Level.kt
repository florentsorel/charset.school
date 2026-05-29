package school.charset.app.domain.exercise.generator

enum class Utf16Level(val number: Int) {
    Bmp(1), // U+0000..U+FFFF excluding surrogates (single 16-bit code unit, 2 bytes)
    Supplementary(2), // U+10000..U+10FFFF (surrogate pair, 4 bytes)
    ;

    val distribution: Map<Utf16Level, Int>
        get() = when (this) {
            Bmp -> mapOf(Bmp to 100)
            Supplementary -> mapOf(Bmp to 30, Supplementary to 70)
        }

    companion object {
        fun fromNumber(n: Int): Utf16Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
