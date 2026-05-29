package school.charset.app.domain.exercise

enum class StepType(val id: String) {
    Format("format"),
    Binary("binary"),
    BitGroups("bit-groups"),
    HexBytes("hex-bytes"),
    CodePointEntry("code-point"),
    UsefulBitCount("useful-bit-count"),
    Endianness("endianness"),
    ;

    companion object {
        fun fromId(id: String): StepType? = entries.firstOrNull { it.id == id }
    }
}
