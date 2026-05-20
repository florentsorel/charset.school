package school.charset.app.domain.exercise

enum class Granularity(val id: String) {
    Verbose("verbose"),
    Standard("standard"),
    Compact("compact"),
    ;

    companion object {
        fun fromId(id: String): Granularity? = entries.firstOrNull { it.id == id }
    }
}
