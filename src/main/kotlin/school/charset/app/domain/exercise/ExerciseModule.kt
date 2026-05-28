package school.charset.app.domain.exercise

enum class ExerciseModule(val id: String, val direction: Direction) {
    Utf8Encode("utf8-encode", Direction.Encode),
    Utf8Decode("utf8-decode", Direction.Decode),
    Utf16Encode("utf16-encode", Direction.Encode),
    Utf16Decode("utf16-decode", Direction.Decode),
    Utf32Encode("utf32-encode", Direction.Encode),
    Utf32Decode("utf32-decode", Direction.Decode),
    Latin1Encode("latin1-encode", Direction.Encode),
    Latin1Decode("latin1-decode", Direction.Decode),
    Windows1252Encode("windows1252-encode", Direction.Encode),
    Windows1252Decode("windows1252-decode", Direction.Decode),
    ;

    enum class Direction { Encode, Decode }

    companion object {
        fun fromId(id: String): ExerciseModule? = entries.firstOrNull { it.id == id }
    }
}
