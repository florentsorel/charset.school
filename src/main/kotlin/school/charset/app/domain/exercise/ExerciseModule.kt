package school.charset.app.domain.exercise

// `maxLevel` mirrors the per-encoding Level enums used by the matching
// generator (Utf8Level / Utf16Level / Utf32Level / Latin1Level /
// Windows1252Level). The Progress layer reads it to cap auto-advance,
// and the front renders the progression indicator against it ("Niveau X
// / maxLevel"). Single source of truth for the encoding's tier count.
enum class ExerciseModule(val id: String, val direction: Direction, val maxLevel: Int) {
    Utf8Encode("utf8-encode", Direction.Encode, maxLevel = 4),
    Utf8Decode("utf8-decode", Direction.Decode, maxLevel = 4),
    Utf16Encode("utf16-encode", Direction.Encode, maxLevel = 2),
    Utf16Decode("utf16-decode", Direction.Decode, maxLevel = 2),
    Utf32Encode("utf32-encode", Direction.Encode, maxLevel = 2),
    Utf32Decode("utf32-decode", Direction.Decode, maxLevel = 2),
    Latin1Encode("latin1-encode", Direction.Encode, maxLevel = 2),
    Latin1Decode("latin1-decode", Direction.Decode, maxLevel = 2),
    Windows1252Encode("windows1252-encode", Direction.Encode, maxLevel = 2),
    Windows1252Decode("windows1252-decode", Direction.Decode, maxLevel = 2),
    ;

    enum class Direction { Encode, Decode }

    companion object {
        fun fromId(id: String): ExerciseModule? = entries.firstOrNull { it.id == id }
    }
}
