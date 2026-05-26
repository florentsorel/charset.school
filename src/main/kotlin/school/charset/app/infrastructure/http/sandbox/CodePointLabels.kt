package school.charset.app.infrastructure.http.sandbox

/**
 * Human-readable labels for code points whose glyph is missing or renders
 * invisibly (control characters, space). The sandbox shows the label in
 * place of the empty/invisible glyph so the user can see *which* code point
 * they typed.
 *
 * Sources: ISO/IEC 6429 (ECMA-48) mnemonics for C0/DEL/C1 controls, plus a
 * special-case "SPACE" for U+0020 (whose glyph is a literal space and
 * therefore invisible in the UI).
 */
internal object CodePointLabels {
    fun lookup(codePoint: Int): String? = when (codePoint) {
        in 0x00..0x1F -> C0[codePoint]
        0x20 -> "SPACE"
        0x7F -> "DEL"
        in 0x80..0x9F -> C1[codePoint - 0x80]
        else -> null
    }

    private val C0 = arrayOf(
        "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL",
        "BS", "HT", "LF", "VT", "FF", "CR", "SO", "SI",
        "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB",
        "CAN", "EM", "SUB", "ESC", "FS", "GS", "RS", "US",
    )

    private val C1 = arrayOf(
        "PAD", "HOP", "BPH", "NBH", "IND", "NEL", "SSA", "ESA",
        "HTS", "HTJ", "VTS", "PLD", "PLU", "RI", "SS2", "SS3",
        "DCS", "PU1", "PU2", "STS", "CCH", "MW", "SPA", "EPA",
        "SOS", "SGC", "SCI", "CSI", "ST", "OSC", "PM", "APC",
    )
}
