package school.charset.app.domain.encoding

enum class Encoding(val id: String) {
    Ascii("ascii"),
    Latin1("latin1"),
    Windows1252("windows-1252"),
    Utf8("utf-8"),
    Utf16Be("utf-16be"),
    Utf16Le("utf-16le"),
    Utf32Be("utf-32be"),
    Utf32Le("utf-32le"),
    ;

    companion object {
        fun fromId(id: String): Encoding? = entries.firstOrNull { it.id == id }
    }

    enum class Endian {
        BigEndian,
        LittleEndian,
    }
}
