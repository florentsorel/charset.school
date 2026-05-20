package school.charset.app.domain.exercise.generator

enum class AsciiLevel(val number: Int) {
    Printable(1), // U+0020..U+007E
    Full(2), // U+0000..U+007F
    ;

    companion object {
        fun fromNumber(n: Int): AsciiLevel? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
