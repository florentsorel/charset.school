package school.charset.app.infrastructure.http.exercise

import school.charset.app.domain.exercise.Step

sealed class ExerciseStepDto {
    abstract val type: String

    data class Format(val choices: List<String>) : ExerciseStepDto() {
        override val type: String = "format"
    }

    data class Binary(val length: Int) : ExerciseStepDto() {
        override val type: String = "binary"
    }

    data class BitGroups(val groupLengths: List<Int>) : ExerciseStepDto() {
        override val type: String = "bit-groups"
    }

    data class HexBytes(val byteCount: Int) : ExerciseStepDto() {
        override val type: String = "hex-bytes"
    }

    data object CodePointEntry : ExerciseStepDto() {
        override val type: String = "code-point"
    }

    data object UsefulBitCount : ExerciseStepDto() {
        override val type: String = "useful-bit-count"
    }

    data object Endianness : ExerciseStepDto() {
        override val type: String = "endianness"
    }
}

fun Step.toDto(): ExerciseStepDto = when (this) {
    is Step.Format -> ExerciseStepDto.Format(choices = choices)
    is Step.Binary -> ExerciseStepDto.Binary(length = length)
    is Step.BitGroups -> ExerciseStepDto.BitGroups(groupLengths = expected.map { it.length })
    is Step.HexBytes -> ExerciseStepDto.HexBytes(byteCount = expected.size)
    is Step.CodePointEntry -> ExerciseStepDto.CodePointEntry
    is Step.UsefulBitCount -> ExerciseStepDto.UsefulBitCount
    is Step.Endianness -> ExerciseStepDto.Endianness
}
