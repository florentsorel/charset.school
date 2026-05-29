package school.charset.app.domain.sandbox

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.exercise.generator.Latin1Generator
import school.charset.app.domain.exercise.generator.Utf16Generator
import school.charset.app.domain.exercise.generator.Utf32Generator
import school.charset.app.domain.exercise.generator.Utf8Generator
import school.charset.app.domain.exercise.generator.Windows1252Generator

class SandboxService(
    private val utf8Generator: Utf8Generator,
    private val utf16Generator: Utf16Generator,
    private val utf32Generator: Utf32Generator,
    private val windows1252Generator: Windows1252Generator,
    private val latin1Generator: Latin1Generator,
) {
    fun encodeUtf8(codePoint: CodePoint): List<Step> = utf8Generator.buildEncodeStepsFor(codePoint)

    fun decodeUtf8(bytes: ByteArray, codePoint: CodePoint): List<Step> = utf8Generator.buildDecodeStepsFor(bytes, codePoint)

    fun encodeUtf16(codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf16Generator.buildEncodeStepsFor(codePoint, endian)

    fun decodeUtf16(bytes: ByteArray, codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf16Generator.buildDecodeStepsFor(bytes, codePoint, endian)

    fun encodeUtf32(codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf32Generator.buildEncodeStepsFor(codePoint, endian)

    fun decodeUtf32(bytes: ByteArray, codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf32Generator.buildDecodeStepsFor(bytes, codePoint, endian)

    fun encodeWindows1252(codePoint: CodePoint): List<Step> = windows1252Generator.buildEncodeStepsFor(codePoint)

    fun decodeWindows1252(bytes: ByteArray, codePoint: CodePoint): List<Step> = windows1252Generator.buildDecodeStepsFor(bytes, codePoint)

    fun encodeLatin1(codePoint: CodePoint): List<Step> = latin1Generator.buildEncodeStepsFor(codePoint)

    fun decodeLatin1(bytes: ByteArray): List<Step> = latin1Generator.buildDecodeStepsFor(bytes)
}
