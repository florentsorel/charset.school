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
    val count: Int? = null,
) {
    fun toDomain(): Answer = when (type) {
        "format" -> Answer.FormatChoice(value ?: missing("value"))
        "binary" -> Answer.BinaryValue(bits ?: missing("bits"))
        "bit-groups" -> Answer.BitGroupsValue(groups ?: missing("groups"))
        "hex-bytes" -> Answer.HexBytesValue(bytes ?: missing("bytes"))
        "code-point" -> Answer.CodePointValue(codePoint ?: missing("codePoint"))
        "useful-bit-count" -> Answer.UsefulBitCountValue(count ?: missing("count"))
        "endianness" -> Answer.EndiannessChoice(
            when (value) {
                "big" -> Encoding.Endian.BigEndian
                "little" -> Encoding.Endian.LittleEndian
                null -> missing("value")
                else -> throw InvalidAnswerPayloadException(type, "value must be 'big' or 'little', got '$value'")
            },
        )
        else -> throw InvalidAnswerPayloadException(type, "unknown answer type")
    }

    private fun missing(field: String): Nothing = throw InvalidAnswerPayloadException(type, "missing required field '$field'")
}

class InvalidAnswerPayloadException(val answerType: String, val reason: String) : RuntimeException("Invalid answer payload (type=$answerType): $reason")
