package school.charset.app.domain.encoding

class EncodingService {
    fun encode(codePoint: CodePoint, encoding: Encoding): ByteArray = when (encoding) {
        Encoding.Ascii -> codePoint.toAscii()
        Encoding.Latin1 -> codePoint.toLatin1()
        Encoding.Windows1252 -> codePoint.toWindows1252()
        Encoding.Utf8 -> codePoint.toUtf8()
        Encoding.Utf16Be, Encoding.Utf16Le -> codePoint.toUtf16(encoding)
        Encoding.Utf32Be, Encoding.Utf32Le -> codePoint.toUtf32(encoding)
    }

    private fun CodePoint.toAscii(): ByteArray {
        if (value !in 0x00..0x7F) {
            throw EncodingException(this, Encoding.Ascii, "value exceeds U+007F")
        }

        return byteArrayOf(value.toByte())
    }

    private fun CodePoint.toLatin1(): ByteArray {
        if (value !in 0x00..0x00FF) {
            throw EncodingException(this, Encoding.Latin1, "value exceeds U+00FF")
        }

        return byteArrayOf(value.toByte())
    }

    private fun CodePoint.toWindows1252(): ByteArray {
        // Identity with Latin-1 outside the 0x80..0x9F special range.
        if (value <= 0x7F || value in 0xA0..0xFF) {
            return byteArrayOf(value.toByte())
        }

        // 27 special mappings in 0x80..0x9F + 5 unassigned bytes (0x81, 0x8D, 0x8F, 0x90, 0x9D).
        val byte = WINDOWS_1252_REVERSE[value]
            ?: throw EncodingException(this, Encoding.Windows1252, "not representable in Windows-1252")

        return byteArrayOf(byte)
    }

    private fun CodePoint.toUtf8(): ByteArray {
        if (isSurrogate) {
            throw EncodingException(this, Encoding.Utf8, "surrogate not encodable in UTF-8")
        }

        return when {
            value <= 0x7F -> byteArrayOf(value.toByte())

            value <= 0x7FF -> byteArrayOf(
                (0xC0 or (value shr 6)).toByte(),
                (0x80 or (value and 0x3F)).toByte(),
            )

            value <= 0xFFFF -> byteArrayOf(
                (0xE0 or (value shr 12)).toByte(),
                (0x80 or ((value shr 6) and 0x3F)).toByte(),
                (0x80 or (value and 0x3F)).toByte(),
            )

            else -> byteArrayOf(
                (0xF0 or (value shr 18)).toByte(),
                (0x80 or ((value shr 12) and 0x3F)).toByte(),
                (0x80 or ((value shr 6) and 0x3F)).toByte(),
                (0x80 or (value and 0x3F)).toByte(),
            )
        }
    }

    private fun CodePoint.toUtf16(encoding: Encoding): ByteArray {
        if (isSurrogate) {
            throw EncodingException(this, encoding, "surrogate not encodable standalone")
        }

        val endian = when (encoding) {
            Encoding.Utf16Be -> Encoding.Endian.BigEndian
            Encoding.Utf16Le -> Encoding.Endian.LittleEndian
            else -> error("toUtf16 called with non-UTF-16 encoding: $encoding")
        }

        return if (isBmp) {
            encodeCodeUnit(value, endian)
        } else {
            val offset = value - 0x10000
            val high = 0xD800 or (offset shr 10)
            val low = 0xDC00 or (offset and 0x3FF)
            encodeCodeUnit(high, endian) + encodeCodeUnit(low, endian)
        }
    }

    private fun CodePoint.toUtf32(encoding: Encoding): ByteArray {
        if (isSurrogate) {
            throw EncodingException(this, encoding, "surrogate not encodable in UTF-32")
        }

        val endian = when (encoding) {
            Encoding.Utf32Be -> Encoding.Endian.BigEndian
            Encoding.Utf32Le -> Encoding.Endian.LittleEndian
            else -> error("toUtf32 called with non-UTF-32 encoding: $encoding")
        }

        val byte0 = (value shr 24).toByte()
        val byte1 = (value shr 16).toByte()
        val byte2 = (value shr 8).toByte()
        val byte3 = value.toByte()

        return when (endian) {
            Encoding.Endian.BigEndian -> byteArrayOf(byte0, byte1, byte2, byte3)
            Encoding.Endian.LittleEndian -> byteArrayOf(byte3, byte2, byte1, byte0)
        }
    }

    private fun encodeCodeUnit(unit: Int, endian: Encoding.Endian): ByteArray {
        val highByte = (unit shr 8).toByte()
        val lowByte = unit.toByte()
        return when (endian) {
            Encoding.Endian.BigEndian -> byteArrayOf(highByte, lowByte)
            Encoding.Endian.LittleEndian -> byteArrayOf(lowByte, highByte)
        }
    }

    private companion object {
        // Reverse map: Unicode code point -> Windows-1252 byte in 0x80..0x9F.
        // Bytes 0x81, 0x8D, 0x8F, 0x90, 0x9D have no Unicode mapping in Windows-1252.
        private val WINDOWS_1252_REVERSE: Map<Int, Byte> = mapOf(
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
    }
}
