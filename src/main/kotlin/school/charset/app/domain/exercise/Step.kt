package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding

sealed class Step {
    abstract val type: StepType

    data class Format(
        val choices: List<String>,
        val expected: String,
    ) : Step() {
        override val type: StepType = StepType.Format
    }

    data class Binary(
        val expected: String,
        val length: Int,
    ) : Step() {
        override val type: StepType = StepType.Binary
    }

    data class BitGroups(
        val expected: List<String>,
    ) : Step() {
        override val type: StepType = StepType.BitGroups
    }

    data class HexBytes(
        val expected: List<Int>,
    ) : Step() {
        override val type: StepType = StepType.HexBytes
    }

    data class CodePointEntry(
        val expected: Int,
    ) : Step() {
        override val type: StepType = StepType.CodePointEntry
    }

    data class Endianness(
        val expected: Encoding.Endian,
    ) : Step() {
        override val type: StepType = StepType.Endianness
    }
}
