package school.charset.app.domain.sandbox

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.exercise.generator.Utf16Generator
import school.charset.app.domain.exercise.generator.Utf8Generator

class SandboxService(
    private val utf8Generator: Utf8Generator,
    private val utf16Generator: Utf16Generator,
) {
    fun encodeUtf8Verbose(codePoint: CodePoint): List<Step> = utf8Generator.buildEncodeStepsFor(codePoint, Granularity.Verbose)

    fun decodeUtf8Verbose(bytes: ByteArray, codePoint: CodePoint): List<Step> = utf8Generator.buildDecodeStepsFor(bytes, codePoint, Granularity.Verbose)

    fun encodeUtf16Verbose(codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf16Generator.buildEncodeStepsFor(codePoint, endian, Granularity.Verbose)

    fun decodeUtf16Verbose(bytes: ByteArray, codePoint: CodePoint, endian: Encoding.Endian): List<Step> = utf16Generator.buildDecodeStepsFor(bytes, codePoint, endian, Granularity.Verbose)
}
