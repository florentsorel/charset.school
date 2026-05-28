package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf16Generator(
    private val codec: Codec,
) {
    fun buildEncodeStepsFor(
        codePoint: CodePoint,
        endian: Encoding.Endian,
        granularity: Granularity,
    ): List<Step> {
        val encoding = endian.toUtf16Encoding()
        val bytes = codec.encode(codePoint, encoding)
        val codeUnitCount = bytes.size / 2
        val hexBytes = bytes.map { it.toInt() and 0xFF }

        val endianStep = Step.Endianness(expected = endian)
        val formatStep = Step.Format(
            choices = CODE_UNIT_CHOICES,
            expected = CODE_UNIT_CHOICES[codeUnitCount - 1],
        )
        val hexStep = Step.HexBytes(expected = hexBytes)

        return when (granularity) {
            Granularity.Verbose -> verboseEncodeSteps(codePoint, codeUnitCount, endianStep, formatStep, hexStep)
            Granularity.Standard -> listOf(endianStep, formatStep, hexStep)
            Granularity.Compact -> listOf(hexStep)
        }
    }

    fun buildDecodeStepsFor(
        bytes: ByteArray,
        codePoint: CodePoint,
        endian: Encoding.Endian,
        granularity: Granularity,
    ): List<Step> {
        val codeUnitCount = bytes.size / 2
        val endianStep = Step.Endianness(expected = endian)
        val formatStep = Step.Format(
            choices = CODE_UNIT_CHOICES,
            expected = CODE_UNIT_CHOICES[codeUnitCount - 1],
        )
        val codePointStep = Step.CodePointEntry(expected = codePoint.value)

        return when (granularity) {
            Granularity.Verbose -> verboseDecodeSteps(codePoint, codeUnitCount, endianStep, formatStep, codePointStep)
            Granularity.Standard -> listOf(endianStep, formatStep, codePointStep)
            Granularity.Compact -> listOf(codePointStep)
        }
    }

    private fun verboseEncodeSteps(
        codePoint: CodePoint,
        codeUnitCount: Int,
        endianStep: Step.Endianness,
        formatStep: Step.Format,
        hexStep: Step.HexBytes,
    ): List<Step> = if (codeUnitCount == 1) {
        val binary = codePoint.value.toString(2).padStart(BMP_DATA_BITS, '0')
        val binaryStep = Step.Binary(expected = binary, length = BMP_DATA_BITS)
        listOf(endianStep, formatStep, binaryStep, hexStep)
    } else {
        val offset = codePoint.value - SUPPLEMENTARY_OFFSET
        val binary = offset.toString(2).padStart(SUPPLEMENTARY_DATA_BITS, '0')
        val binaryStep = Step.Binary(expected = binary, length = SUPPLEMENTARY_DATA_BITS)
        val bitGroupsStep = Step.BitGroups(
            expected = listOf(binary.substring(0, 10), binary.substring(10, 20)),
        )
        listOf(endianStep, formatStep, binaryStep, bitGroupsStep, hexStep)
    }

    private fun verboseDecodeSteps(
        codePoint: CodePoint,
        codeUnitCount: Int,
        endianStep: Step.Endianness,
        formatStep: Step.Format,
        codePointStep: Step.CodePointEntry,
    ): List<Step> = if (codeUnitCount == 1) {
        val binary = codePoint.value.toString(2).padStart(BMP_DATA_BITS, '0')
        val binaryStep = Step.Binary(expected = binary, length = BMP_DATA_BITS)
        listOf(endianStep, formatStep, binaryStep, codePointStep)
    } else {
        val offset = codePoint.value - SUPPLEMENTARY_OFFSET
        val binary = offset.toString(2).padStart(SUPPLEMENTARY_DATA_BITS, '0')
        val binaryStep = Step.Binary(expected = binary, length = SUPPLEMENTARY_DATA_BITS)
        val bitGroupsStep = Step.BitGroups(
            expected = listOf(binary.substring(0, 10), binary.substring(10, 20)),
        )
        listOf(endianStep, formatStep, bitGroupsStep, binaryStep, codePointStep)
    }

    private fun Encoding.Endian.toUtf16Encoding(): Encoding = when (this) {
        Encoding.Endian.BigEndian -> Encoding.Utf16Be
        Encoding.Endian.LittleEndian -> Encoding.Utf16Le
    }

    private companion object {
        private const val BMP_DATA_BITS = 16
        private const val SUPPLEMENTARY_DATA_BITS = 20
        private const val SUPPLEMENTARY_OFFSET = 0x10000

        private val CODE_UNIT_CHOICES = listOf(
            FormatChoice.ONE_CODE_UNIT,
            FormatChoice.TWO_CODE_UNITS,
        )
    }
}
