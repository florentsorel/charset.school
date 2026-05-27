package school.charset.app.infrastructure.http.sandbox

/**
 * Human-readable labels for code points whose glyph would be invisible or
 * absent (control characters, whitespace, private use area, format
 * characters, combining marks, non-characters, unassigned slots, ...).
 *
 * Acts as the single source of truth for "should the front render a
 * glyph or a label?": when `lookup(cp) != null`, the response sets
 * `glyph = null` and the front shows the returned label instead.
 *
 * Sources:
 *   - ISO/IEC 6429 (ECMA-48) mnemonics for C0/DEL/C1
 *   - "SPACE" for U+0020 (would render as a literal blank)
 *   - Common short mnemonics for named format/whitespace chars (NBSP,
 *     ZWJ, BOM, etc.) - see `NAMED`
 *   - Category-based fallback via `java.lang.Character.getType` for
 *     PUA / FORMAT / NON_SPACING_MARK / unassigned / ...
 *   - Explicit handling of Unicode non-characters (U+FDD0..U+FDEF and
 *     U+nFFFE / U+nFFFF) which `Character.getType` reports as
 *     unassigned but deserve a distinct label.
 */
internal object CodePointLabels {
    fun lookup(codePoint: Int): String? = when (codePoint) {
        in 0x00..0x1F -> C0[codePoint]
        0x20 -> "SPACE"
        0x7F -> "DEL"
        in 0x80..0x9F -> C1[codePoint - 0x80]
        in NAMED -> NAMED[codePoint]
        else -> categoryLabel(codePoint)
    }

    private fun categoryLabel(codePoint: Int): String? {
        // Unicode non-characters: U+FDD0..U+FDEF and the last two code
        // points of every plane. `Character.getType` reports these as
        // UNASSIGNED on most JDKs, so check first.
        if (codePoint in 0xFDD0..0xFDEF || (codePoint and 0xFFFE) == 0xFFFE) {
            return "NONCHAR"
        }
        return when (Character.getType(codePoint).toByte()) {
            Character.UNASSIGNED -> "UNASSIGNED"
            Character.PRIVATE_USE -> "PUA"
            Character.FORMAT -> "FORMAT"
            Character.LINE_SEPARATOR -> "LSEP"
            Character.PARAGRAPH_SEPARATOR -> "PSEP"
            Character.SPACE_SEPARATOR -> "WHITESPACE"
            Character.NON_SPACING_MARK -> "COMBINING"
            Character.ENCLOSING_MARK -> "COMBINING"
            Character.COMBINING_SPACING_MARK -> "COMBINING"
            else -> null
        }
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

    // Named format / invisible / bidi chars where a short mnemonic exists
    // in Unicode literature. More precise than the category fallback.
    private val NAMED = mapOf(
        0x00A0 to "NBSP", // No-break space
        0x00AD to "SHY", // Soft hyphen
        0x034F to "CGJ", // Combining grapheme joiner
        0x180E to "MVS", // Mongolian vowel separator
        0x200B to "ZWSP", // Zero-width space
        0x200C to "ZWNJ", // Zero-width non-joiner
        0x200D to "ZWJ", // Zero-width joiner
        0x200E to "LRM", // Left-to-right mark
        0x200F to "RLM", // Right-to-left mark
        0x2028 to "LSEP", // Line separator
        0x2029 to "PSEP", // Paragraph separator
        0x202A to "LRE",
        0x202B to "RLE",
        0x202C to "PDF",
        0x202D to "LRO",
        0x202E to "RLO",
        0x2060 to "WJ", // Word joiner
        0x2066 to "LRI",
        0x2067 to "RLI",
        0x2068 to "FSI",
        0x2069 to "PDI",
        0xFEFF to "BOM", // Byte order mark / zero-width no-break space
    )
}
