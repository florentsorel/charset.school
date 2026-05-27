package school.charset.app.domain.sandbox

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.exercise.generator.Utf8Generator

class SandboxService(
    private val utf8Generator: Utf8Generator,
) {
    fun encodeUtf8Verbose(codePoint: CodePoint): List<Step> = utf8Generator.buildEncodeStepsFor(codePoint, Granularity.Verbose)

    fun decodeUtf8Verbose(bytes: ByteArray, codePoint: CodePoint): List<Step> = utf8Generator.buildDecodeStepsFor(bytes, codePoint, Granularity.Verbose)
}
