package school.charset.app.domain.encoding

class Codec {
    fun encode(codePoint: CodePoint, encoding: Encoding): ByteArray = when (encoding) {
        Encoding.Ascii -> codePoint.toAscii()
        Encoding.Latin1 -> codePoint.toLatin1()
        Encoding.Windows1252 -> codePoint.toWindows1252()
        Encoding.Utf8 -> codePoint.toUtf8()
        Encoding.Utf16Be, Encoding.Utf16Le -> codePoint.toUtf16(encoding)
        Encoding.Utf32Be, Encoding.Utf32Le -> codePoint.toUtf32(encoding)
    }

    fun decode(bytes: ByteArray, encoding: Encoding): CodePoint = when (encoding) {
        Encoding.Ascii -> bytes.fromAscii()
        Encoding.Latin1 -> bytes.fromLatin1()
        Encoding.Windows1252 -> bytes.fromWindows1252()
        Encoding.Utf8 -> bytes.fromUtf8()
        Encoding.Utf16Be, Encoding.Utf16Le -> bytes.fromUtf16(encoding)
        Encoding.Utf32Be, Encoding.Utf32Le -> bytes.fromUtf32(encoding)
    }

    private fun CodePoint.toAscii(): ByteArray {
        if (value !in 0x00..0x7F) {
            throw EncoderException(this, Encoding.Ascii, "value exceeds U+007F")
        }

        return byteArrayOf(value.toByte())
    }

    private fun CodePoint.toLatin1(): ByteArray {
        if (value !in 0x00..0x00FF) {
            throw EncoderException(this, Encoding.Latin1, "value exceeds U+00FF")
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
            ?: throw EncoderException(this, Encoding.Windows1252, "not representable in Windows-1252")

        return byteArrayOf(byte)
    }

    private fun CodePoint.toUtf8(): ByteArray {
        if (isSurrogate) {
            throw EncoderException(this, Encoding.Utf8, "surrogate not encodable in UTF-8")
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
            throw EncoderException(this, encoding, "surrogate not encodable standalone")
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
            throw EncoderException(this, encoding, "surrogate not encodable in UTF-32")
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

    private fun ByteArray.fromAscii(): CodePoint {
        if (size != 1) {
            throw DecoderException(this, Encoding.Ascii, "expected exactly 1 byte, got $size")
        }

        // Unsigned value
        val value = this[0].toInt() and 0xFF

        if (value > 0x7F) {
            throw DecoderException(this, Encoding.Ascii, "high bit set, not ASCII")
        }

        return CodePoint(value)
    }

    private fun ByteArray.fromLatin1(): CodePoint {
        if (size != 1) {
            throw DecoderException(this, Encoding.Latin1, "expected exactly 1 byte, got $size")
        }

        return CodePoint(this[0].toInt() and 0xFF)
    }

    private fun ByteArray.fromWindows1252(): CodePoint {
        if (size != 1) {
            throw DecoderException(this, Encoding.Windows1252, "expected exactly 1 byte, got $size")
        }

        // Unsigned value
        val value = this[0].toInt() and 0xFF

        // Identity ranges
        if (value <= 0x7F || value in 0xA0..0xFF) {
            return CodePoint(value)
        }

        val codePoint = WINDOWS_1252_FORWARD[this[0]]
            ?: throw DecoderException(this, Encoding.Windows1252, "byte 0x${"%02X".format(value)} is unassigned in Windows-1252")

        return CodePoint(codePoint)
    }

    private fun ByteArray.fromUtf8(): CodePoint {
        if (isEmpty()) {
            throw DecoderException(this, Encoding.Utf8, "empty input")
        }

        val first = this[0].toInt() and 0xFF

        // Determine expected sequence length from the leading byte pattern.
        val length = when {
            first <= 0x7F -> 1

            first in 0x80..0xBF -> throw DecoderException(
                this,
                Encoding.Utf8,
                "byte 0x${"%02X".format(first)} is a continuation byte, not a valid leader",
            )

            first in 0xC0..0xDF -> 2

            first in 0xE0..0xEF -> 3

            first in 0xF0..0xF7 -> 4

            else -> throw DecoderException(
                this,
                Encoding.Utf8,
                "invalid leading byte 0x${"%02X".format(first)}",
            )
        }

        if (size != length) {
            throw DecoderException(
                this,
                Encoding.Utf8,
                "expected $length bytes for leading byte 0x${"%02X".format(first)}, got $size",
            )
        }

        // Strip the marker bits from the leading byte: 7/5/4/3 data bits respectively.
        val initialMask = when (length) {
            1 -> 0x7F
            2 -> 0x1F
            3 -> 0x0F
            else -> 0x07
        }
        var value = first and initialMask

        // Each continuation byte contributes 6 data bits and must match 10xxxxxx.
        for (i in 1 until length) {
            val byte = this[i].toInt() and 0xFF
            if (byte and 0xC0 != 0x80) {
                throw DecoderException(
                    this,
                    Encoding.Utf8,
                    "byte $i (0x${"%02X".format(byte)}) is not a valid continuation byte",
                )
            }
            value = (value shl 6) or (byte and 0x3F)
        }

        // Reject overlong encodings: the decoded value must require this byte count.
        val minForLength = when (length) {
            1 -> 0x00
            2 -> 0x80
            3 -> 0x800
            else -> 0x10000
        }
        if (value < minForLength) {
            throw DecoderException(
                this,
                Encoding.Utf8,
                "overlong encoding: U+${"%04X".format(value)} should use a shorter form",
            )
        }

        if (value > 0x10FFFF) {
            throw DecoderException(
                this,
                Encoding.Utf8,
                "value U+${"%X".format(value)} exceeds U+10FFFF",
            )
        }

        if (value in 0xD800..0xDFFF) {
            throw DecoderException(
                this,
                Encoding.Utf8,
                "surrogate U+${"%04X".format(value)} not a valid code point",
            )
        }

        return CodePoint(value)
    }

    private fun ByteArray.fromUtf32(encoding: Encoding): CodePoint {
        if (size != 4) {
            throw DecoderException(this, encoding, "expected exactly 4 bytes, got $size")
        }

        // Normalize byte order to BE for a single assembly path.
        val ordered = when (encoding) {
            Encoding.Utf32Be -> this
            Encoding.Utf32Le -> reversedArray()
            else -> error("fromUtf32 called with non-UTF-32 encoding: $encoding")
        }

        val value = ((ordered[0].toInt() and 0xFF) shl 24) or
            ((ordered[1].toInt() and 0xFF) shl 16) or
            ((ordered[2].toInt() and 0xFF) shl 8) or
            (ordered[3].toInt() and 0xFF)

        if (value !in 0..0x10FFFF) {
            throw DecoderException(this, encoding, "value 0x${"%X".format(value)} exceeds U+10FFFF")
        }

        if (value in 0xD800..0xDFFF) {
            throw DecoderException(this, encoding, "surrogate U+${"%04X".format(value)} not a valid code point")
        }

        return CodePoint(value)
    }

    private fun ByteArray.fromUtf16(encoding: Encoding): CodePoint {
        if (size != 2 && size != 4) {
            throw DecoderException(this, encoding, "expected 2 or 4 bytes, got $size")
        }

        val endian = when (encoding) {
            Encoding.Utf16Be -> Encoding.Endian.BigEndian
            Encoding.Utf16Le -> Encoding.Endian.LittleEndian
            else -> error("fromUtf16 called with non-UTF-16 encoding: $encoding")
        }

        return when (val firstUnit = decodeCodeUnit(this, 0, endian)) {
            in 0xD800..0xDBFF -> {
                // High surrogate - must be followed by a low surrogate in a 4-byte input.
                if (size != 4) {
                    throw DecoderException(
                        this,
                        encoding,
                        "high surrogate 0x${"%04X".format(firstUnit)} requires a following low surrogate",
                    )
                }
                val secondUnit = decodeCodeUnit(this, 2, endian)
                if (secondUnit !in 0xDC00..0xDFFF) {
                    throw DecoderException(
                        this,
                        encoding,
                        "high surrogate 0x${"%04X".format(firstUnit)} not followed by low surrogate, got 0x${"%04X".format(secondUnit)}",
                    )
                }
                val codePoint = 0x10000 + ((firstUnit - 0xD800) shl 10) + (secondUnit - 0xDC00)
                CodePoint(codePoint)
            }

            in 0xDC00..0xDFFF -> {
                throw DecoderException(
                    this,
                    encoding,
                    "lone low surrogate 0x${"%04X".format(firstUnit)}",
                )
            }

            else -> {
                // BMP code point - must be exactly 2 bytes (4 would indicate a leftover code unit).
                if (size != 2) {
                    throw DecoderException(
                        this,
                        encoding,
                        "BMP code point 0x${"%04X".format(firstUnit)} must be exactly 2 bytes, got $size",
                    )
                }
                CodePoint(firstUnit)
            }
        }
    }

    private fun decodeCodeUnit(bytes: ByteArray, offset: Int, endian: Encoding.Endian): Int {
        val first = bytes[offset].toInt() and 0xFF
        val second = bytes[offset + 1].toInt() and 0xFF
        return when (endian) {
            Encoding.Endian.BigEndian -> (first shl 8) or second
            Encoding.Endian.LittleEndian -> (second shl 8) or first
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

        private val WINDOWS_1252_FORWARD: Map<Byte, Int> =
            WINDOWS_1252_REVERSE.entries.associate { (cp, b) -> b to cp }
    }
}
