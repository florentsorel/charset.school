package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Step

class Utf32Generator(
    private val codec: Codec,
) {
    fun buildEncodeStepsFor(codePoint: CodePoint, endian: Encoding.Endian): List<Step> {
        val encoding = endian.toUtf32Encoding()
        val bytes = codec.encode(codePoint, encoding)
        val hexBytes = bytes.map { it.toInt() and 0xFF }

        val endianStep = Step.Endianness(expected = endian)
        val binaryStep = Step.Binary(
            expected = codePoint.value.toString(2).padStart(UTF32_BITS, '0'),
            length = UTF32_BITS,
        )
        val hexStep = Step.HexBytes(expected = hexBytes)

        return listOf(endianStep, binaryStep, hexStep)
    }

    fun buildDecodeStepsFor(bytes: ByteArray, codePoint: CodePoint, endian: Encoding.Endian): List<Step> {
        // Reorder the user-facing bytes back to network order before
        // deriving the binary, so the displayed bits actually correspond
        // to the byte sequence the decoder will read.
        val orderedBytes = when (endian) {
            Encoding.Endian.BigEndian -> bytes
            Encoding.Endian.LittleEndian -> bytes.reversedArray()
        }
        val binary = orderedBytes.joinToString("") {
            (it.toInt() and 0xFF).toString(2).padStart(8, '0')
        }

        val endianStep = Step.Endianness(expected = endian)
        val binaryStep = Step.Binary(expected = binary, length = UTF32_BITS)
        val codePointStep = Step.CodePointEntry(expected = codePoint.value)

        return listOf(endianStep, binaryStep, codePointStep)
    }

    private fun Encoding.Endian.toUtf32Encoding(): Encoding = when (this) {
        Encoding.Endian.BigEndian -> Encoding.Utf32Be
        Encoding.Endian.LittleEndian -> Encoding.Utf32Le
    }

    private companion object {
        private const val UTF32_BITS = 32
    }
}
