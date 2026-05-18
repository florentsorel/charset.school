package school.charset.app.domain.encoding

class EncodingService {
    fun encode(codePoint: CodePoint, encoding: Encoding): ByteArray = when (encoding) {
        Encoding.Ascii -> codePoint.toAscii()
        Encoding.Latin1 -> codePoint.toLatin1()
        Encoding.Windows1252 -> TODO("Not yet implemented")
        Encoding.Utf8 -> TODO("Not yet implemented")
        Encoding.Utf16Be -> TODO("Not yet implemented")
        Encoding.Utf16Le -> TODO("Not yet implemented")
        Encoding.Utf32Be -> TODO("Not yet implemented")
        Encoding.Utf32Le -> TODO("Not yet implemented")
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
}
