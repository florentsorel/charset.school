package school.charset.app.domain.exercise

import school.charset.app.domain.encoding.Encoding

sealed class Answer {
    data class FormatChoice(val value: String) : Answer()
    data class BinaryValue(val bits: String) : Answer()
    data class BitGroupsValue(val groups: List<String>) : Answer()
    data class HexBytesValue(val bytes: List<Int>) : Answer()
    data class CodePointValue(val value: Int) : Answer()
    data class EndiannessChoice(val value: Encoding.Endian) : Answer()
}
