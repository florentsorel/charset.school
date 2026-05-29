package school.charset.app.domain.exercise.generator

enum class Windows1252Level(val number: Int) {
    SpecialBlock(1), // the 27 special code points (€, Œ, ™, ...)
    AllEncodable(2), // the 251 encodable code points (ASCII + special + Latin-1 supplement)
    ;

    val distribution: Map<Windows1252Level, Int>
        get() = when (this) {
            SpecialBlock -> mapOf(SpecialBlock to 100)
            AllEncodable -> mapOf(SpecialBlock to 30, AllEncodable to 70)
        }

    companion object {
        fun fromNumber(n: Int): Windows1252Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
