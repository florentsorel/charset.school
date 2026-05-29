package school.charset.app.domain.exercise.generator

enum class Latin1Level(val number: Int) {
    Supplement(1), // U+00A0..U+00FF (printable Latin-1 supplement)
    Full(2), // U+0000..U+00FF
    ;

    val distribution: Map<Latin1Level, Int>
        get() = when (this) {
            Supplement -> mapOf(Supplement to 100)
            Full -> mapOf(Supplement to 40, Full to 60)
        }

    companion object {
        fun fromNumber(n: Int): Latin1Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
