package school.charset.app.infrastructure.http.exercise

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer

data class GenerateExerciseRequest(
    @field:NotBlank
    val moduleId: String,
    @field:Min(1) @field:Max(10)
    val level: Int,
    @field:NotBlank
    val granularity: String,
)

data class ValidateStepRequest(
    @field:NotNull
    val attemptId: Long,
    @field:Min(0)
    val stepIndex: Int,
    @field:Valid @field:NotNull
    val answer: AnswerWire,
)

data class RevealStepRequest(
    @field:NotNull
    val attemptId: Long,
    @field:Min(0)
    val stepIndex: Int,
)

data class AnswerWire(
    @field:NotBlank
    val type: String,
    val value: String? = null,
    val bits: String? = null,
    val groups: List<String>? = null,
    val bytes: List<Int>? = null,
    val codePoint: Int? = null,
) {
    fun toDomain(): Answer = when (type) {
        "format" -> Answer.FormatChoice(requireNotNull(value) { "format answer requires 'value'" })
        "binary" -> Answer.BinaryValue(requireNotNull(bits) { "binary answer requires 'bits'" })
        "bit-groups" -> Answer.BitGroupsValue(requireNotNull(groups) { "bit-groups answer requires 'groups'" })
        "hex-bytes" -> Answer.HexBytesValue(requireNotNull(bytes) { "hex-bytes answer requires 'bytes'" })
        "code-point" -> Answer.CodePointValue(requireNotNull(codePoint) { "code-point answer requires 'codePoint'" })
        "endianness" -> Answer.EndiannessChoice(
            when (value) {
                "big" -> Encoding.Endian.BigEndian
                "little" -> Encoding.Endian.LittleEndian
                else -> error("endianness answer requires value 'big' or 'little'")
            },
        )
        else -> error("unknown answer type: $type")
    }
}
