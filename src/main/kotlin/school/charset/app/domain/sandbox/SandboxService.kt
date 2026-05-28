package school.charset.app.domain.sandbox

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Granularity
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
    fun encodeUtf8Verbose(codePoint: CodePoint): List<Step> = utf8Generator.buildEncodeStepsFor(codePoint, Granularity.Verbose)

    fun decodeUtf8Verbose(bytes: ByteArray, codePoint: CodePoint): List<Step> = utf8Generator.buildDecodeStepsFor(bytes, codePoint, Granularity.Verbose)

    fun encodeUtf16Verbose(codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf16Generator.buildEncodeStepsFor(codePoint, endian, Granularity.Verbose)

    fun decodeUtf16Verbose(bytes: ByteArray, codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf16Generator.buildDecodeStepsFor(bytes, codePoint, endian, Granularity.Verbose)

    fun encodeUtf32Verbose(codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf32Generator.buildEncodeStepsFor(codePoint, endian, Granularity.Verbose)

    fun decodeUtf32Verbose(bytes: ByteArray, codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf32Generator.buildDecodeStepsFor(bytes, codePoint, endian, Granularity.Verbose)

    fun encodeWindows1252Verbose(codePoint: CodePoint): List<Step> = windows1252Generator.buildEncodeStepsFor(codePoint, Granularity.Verbose)

    fun decodeWindows1252Verbose(bytes: ByteArray, codePoint: CodePoint): List<Step> = windows1252Generator.buildDecodeStepsFor(bytes, codePoint, Granularity.Verbose)

    fun encodeLatin1Verbose(codePoint: CodePoint): List<Step> = latin1Generator.buildEncodeStepsFor(codePoint, Granularity.Verbose)

    fun decodeLatin1Verbose(bytes: ByteArray): List<Step> = latin1Generator.buildDecodeStepsFor(bytes, Granularity.Verbose)
}
