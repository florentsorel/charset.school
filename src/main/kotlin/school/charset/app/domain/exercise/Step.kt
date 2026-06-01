package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding

sealed class Step {
    abstract val type: StepType

    data class Format(
        val choices: List<String>,
        val expected: String,
    ) : Step() {
        override val type: StepType = StepType.Format

        init {
            require(choices.isNotEmpty()) { "Format step must offer at least one choice" }
            require(expected in choices) {
                "Format step expected '$expected' must be one of the offered choices"
            }
        }
    }

    data class Binary(
        val expected: String,
        val length: Int,
    ) : Step() {
        override val type: StepType = StepType.Binary

        init {
            require(length > 0) { "Binary step length must be positive, got $length" }
            require(expected.length == length) {
                "Binary step expected length must equal length: expected.length=${expected.length}, length=$length"
            }
            require(expected.all { it == '0' || it == '1' }) {
                "Binary step expected must contain only 0/1 characters"
            }
        }
    }

    data class BitGroups(
        val expected: List<String>,
    ) : Step() {
        override val type: StepType = StepType.BitGroups

        init {
            require(expected.isNotEmpty()) { "BitGroups step must have at least one group" }
            expected.forEachIndexed { index, group ->
                require(group.isNotEmpty()) { "BitGroups group at position $index is empty" }
                require(group.all { it == '0' || it == '1' }) {
                    "BitGroups group at position $index must contain only 0/1 characters"
                }
            }
        }
    }

    data class HexBytes(
        val expected: List<Int>,
    ) : Step() {
        override val type: StepType = StepType.HexBytes

        init {
            require(expected.isNotEmpty()) { "HexBytes step must have at least one byte" }
            expected.forEachIndexed { index, byte ->
                require(byte in 0..0xFF) {
                    "HexBytes step byte at position $index must be in 0..255, got $byte"
                }
            }
        }
    }

    data class CodePointEntry(
        val expected: Int,
    ) : Step() {
        override val type: StepType = StepType.CodePointEntry

        init {
            require(expected in 0..0x10FFFF) {
                "CodePoint step expected must be in Unicode range, got $expected"
            }
            require(expected !in 0xD800..0xDFFF) {
                "CodePoint step expected must not be a surrogate, got U+${"%04X".format(expected)}"
            }
        }
    }

    data class UsefulBitCount(
        val expected: Int,
    ) : Step() {
        override val type: StepType = StepType.UsefulBitCount

        init {
            require(expected in 1..32) {
                "UsefulBitCount expected must be in 1..32, got $expected"
            }
        }
    }

    data class Endianness(
        val expected: Encoding.Endian,
    ) : Step() {
        override val type: StepType = StepType.Endianness
    }

    data class Offset(
        val expected: Int,
    ) : Step() {
        override val type: StepType = StepType.Offset

        init {
            require(expected in 0..0xFFFFF) {
                "Offset step expected must be a 20-bit value (0..0xFFFFF), got $expected"
            }
        }
    }
}
