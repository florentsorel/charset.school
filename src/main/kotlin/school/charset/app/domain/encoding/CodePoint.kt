package school.charset.app.domain.encoding

/**
 * Unicode code point. The valid range is U+0000..U+10FFFF; values inside the
 * surrogate range U+D800..U+DFFF are still legal `CodePoint`s (they exist in
 * the Unicode standard as reserved values), but they are not valid characters
 * and will be rejected by every encoder - that constraint lives in the encoders.
 */
@JvmInline
value class CodePoint(val value: Int) {
    init {
        require(value in MIN..MAX) { "Code point out of range: 0x${value.toString(16).uppercase()}" }
    }

    val isSurrogate: Boolean
        get() = value in SURROGATE_MIN..SURROGATE_MAX

    val isBmp: Boolean
        get() = value <= BMP_MAX

    override fun toString() = "U+%04X".format(value)

    companion object {
        const val MIN = 0x0000
        const val MAX = 0x10FFFF
        const val SURROGATE_MIN = 0xD800
        const val SURROGATE_MAX = 0xDFFF
        const val BMP_MAX = 0xFFFF // Basic Multilingual Plane
    }
}
