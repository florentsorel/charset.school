package school.charset.app.domain.encoding

/**
 * The Windows-1252 specification, used by both `Codec` (encode/decode) and
 * `CodePointGenerator` (exercise input picking).
 *
 * Three disjoint ranges of encodable code points:
 *
 * 1. ASCII identity: U+0000..U+007F → bytes 0x00..0x7F
 * 2. Special block: 27 scattered code points → bytes 0x80..0x9F
 *    (bytes 0x81, 0x8D, 0x8F, 0x90, 0x9D are unassigned)
 * 3. Latin-1 supplement identity: U+00A0..U+00FF → bytes 0xA0..0xFF
 *
 * Total encodable: 128 + 27 + 96 = 251 code points.
 *
 * The single source of truth is `specialMappings` - an ordered list of
 * (code point, byte) pairs sorted by byte value (0x80, 0x82, ..., 0x9F). The
 * two maps and the public `specialCodePoints` list are derived from it, so the
 * "ordered by byte value" guarantee is explicit and resilient to refactors.
 */
object Windows1252Spec {
    /**
     * The 27 special mappings, ordered by byte value (0x80, 0x82, ..., 0x9F).
     * Bytes 0x81, 0x8D, 0x8F, 0x90, 0x9D are skipped (unassigned in Windows-1252).
     * Single source of truth - all other public members are derived from this.
     */
    private val specialMappings: List<Pair<Int, Byte>> = listOf(
        0x20AC to 0x80.toByte(), // € - Euro Sign
        0x201A to 0x82.toByte(), // ‚ - Single Low-9 Quotation Mark
        0x0192 to 0x83.toByte(), // ƒ - Latin Small Letter F with Hook
        0x201E to 0x84.toByte(), // „ - Double Low-9 Quotation Mark
        0x2026 to 0x85.toByte(), // … - Horizontal Ellipsis
        0x2020 to 0x86.toByte(), // † - Dagger
        0x2021 to 0x87.toByte(), // ‡ - Double Dagger
        0x02C6 to 0x88.toByte(), // ˆ - Modifier Letter Circumflex Accent
        0x2030 to 0x89.toByte(), // ‰ - Per Mille Sign
        0x0160 to 0x8A.toByte(), // Š - Latin Capital Letter S with Caron
        0x2039 to 0x8B.toByte(), // ‹ - Single Left-Pointing Angle Quotation Mark
        0x0152 to 0x8C.toByte(), // Œ - Latin Capital Ligature OE
        0x017D to 0x8E.toByte(), // Ž - Latin Capital Letter Z with Caron
        0x2018 to 0x91.toByte(), // ' - Left Single Quotation Mark
        0x2019 to 0x92.toByte(), // ' - Right Single Quotation Mark
        0x201C to 0x93.toByte(), // " - Left Double Quotation Mark
        0x201D to 0x94.toByte(), // " - Right Double Quotation Mark
        0x2022 to 0x95.toByte(), // • - Bullet
        0x2013 to 0x96.toByte(), // – - En Dash
        0x2014 to 0x97.toByte(), // — - Em Dash
        0x02DC to 0x98.toByte(), // ˜ - Small Tilde
        0x2122 to 0x99.toByte(), // ™ - Trade Mark Sign
        0x0161 to 0x9A.toByte(), // š - Latin Small Letter S with Caron
        0x203A to 0x9B.toByte(), // › - Single Right-Pointing Angle Quotation Mark
        0x0153 to 0x9C.toByte(), // œ - Latin Small Ligature OE
        0x017E to 0x9E.toByte(), // ž - Latin Small Letter Z with Caron
        0x0178 to 0x9F.toByte(), // Ÿ - Latin Capital Letter Y with Diaeresis
    )

    /** Forward map for encoding: code point -> byte (for the special block only). */
    val codePointToByte: Map<Int, Byte> = specialMappings.toMap()

    /** Reverse map for decoding: byte -> code point (for the special block only). */
    val byteToCodePoint: Map<Byte, Int> =
        specialMappings.associate { (cp, b) -> b to cp }

    /**
     * The 27 special code points, ordered by their byte value (0x80, 0x82, ..., 0x9F).
     * Used for level 1 of the Windows-1252 exercise generator.
     */
    val specialCodePoints: List<Int> = specialMappings.map { (cp, _) -> cp }

    /**
     * All 251 encodable code points, ordered as:
     * - U+0000..U+007F (128 ASCII identity entries)
     * - The 27 special code points (in byte order)
     * - U+00A0..U+00FF (96 Latin-1 supplement identity entries)
     */
    val encodableCodePoints: List<Int> =
        (0x00..0x7F).toList() + specialCodePoints + (0xA0..0xFF).toList()
}
