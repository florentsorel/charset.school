package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding

class ByteArrayGenerator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
) {
    fun randomAscii(level: AsciiLevel): ByteArray = codec.encode(codePointGenerator.randomAscii(level), Encoding.Ascii)

    fun randomLatin1(level: Latin1Level): ByteArray = codec.encode(codePointGenerator.randomLatin1(level), Encoding.Latin1)

    fun randomWindows1252(level: Windows1252Level): ByteArray = codec.encode(codePointGenerator.randomWindows1252(level), Encoding.Windows1252)

    fun randomUtf8(level: Utf8Level): ByteArray = codec.encode(codePointGenerator.randomUtf8(level), Encoding.Utf8)

    fun randomUtf16(level: Utf16Level, encoding: Encoding): ByteArray {
        require(encoding in UTF16_ENCODINGS) {
            "randomUtf16 requires Utf16Be or Utf16Le, got ${encoding.id}"
        }
        return codec.encode(codePointGenerator.randomUtf16(level), encoding)
    }

    fun randomUtf32(level: Utf32Level, encoding: Encoding): ByteArray {
        require(encoding in UTF32_ENCODINGS) {
            "randomUtf32 requires Utf32Be or Utf32Le, got ${encoding.id}"
        }
        return codec.encode(codePointGenerator.randomUtf32(level), encoding)
    }

    private companion object {
        private val UTF16_ENCODINGS = setOf(Encoding.Utf16Be, Encoding.Utf16Le)
        private val UTF32_ENCODINGS = setOf(Encoding.Utf32Be, Encoding.Utf32Le)
    }
}
