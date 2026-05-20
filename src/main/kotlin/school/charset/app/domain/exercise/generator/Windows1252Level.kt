package school.charset.app.domain.exercise.generator

enum class Windows1252Level(val number: Int) {
    SpecialBlock(1), // the 27 special code points (€, Œ, ™, ...)
    AllEncodable(2), // the 251 encodable code points (ASCII + special + Latin-1 supplement)
    ;

    companion object {
        fun fromNumber(n: Int): Windows1252Level? = entries.firstOrNull { it.number == n }

        val validNumbers: String = entries.joinToString(", ") { it.number.toString() }
    }
}
