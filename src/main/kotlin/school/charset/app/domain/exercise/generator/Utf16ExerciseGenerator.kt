package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Step

class Utf16ExerciseGenerator(
    private val codec: Codec,
    override val encoding: Encoding,
    private val codePointGenerator: CodePointGenerator,
    private val byteArrayGenerator: ByteArrayGenerator,
    private val utf16Generator: Utf16Generator,
) : EncodingExerciseGenerator {

    init {
        require(encoding == Encoding.Utf16Be || encoding == Encoding.Utf16Le) {
            "Utf16ExerciseGenerator requires Utf16Be or Utf16Le, got ${encoding.id}"
        }
    }

    private val endian: Encoding.Endian =
        if (encoding == Encoding.Utf16Be) Encoding.Endian.BigEndian else Encoding.Endian.LittleEndian

    override fun generateEncode(level: Int): Exercise.Encode {
        val utf16Level = parseLevel(level)
        val codePoint = codePointGenerator.randomUtf16(utf16Level)
        val steps = utf16Generator.buildEncodeStepsFor(codePoint, endian)
            .forExercise()
            .withOffsetStep(codePoint.value)
        return Exercise.Encode(codePoint, encoding, level, steps)
    }

    override fun generateDecode(level: Int): Exercise.Decode {
        val utf16Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf16(utf16Level, encoding)
        val codePoint = codec.decode(bytes, encoding)
        val steps = utf16Generator.buildDecodeStepsFor(bytes, codePoint, endian)
            .forExercise()
            .withDecodeOffsetStep(codePoint.value)
        return Exercise.Decode(bytes, codePoint, encoding, level, steps)
    }

    private fun List<Step>.forExercise(): List<Step> = withoutEndianness().simplifyBmp()

    // The exercise gives the target endianness in its header (a random BE/LE),
    // it isn't something the learner derives - so it's not a step. Byte order is
    // still tested via the hex-bytes step. The sandbox keeps the endianness step
    // (shared Utf16Generator) for its explanation panel.
    private fun List<Step>.withoutEndianness(): List<Step> = filterNot { it is Step.Endianness }

    // A BMP code point is a direct copy: the 16-bit code unit IS the scalar
    // value, so the binary step is just a hex<->binary detour. We drop it and go
    // straight from the format choice to the hex bytes. The surrogate case
    // (supplementary, identified by its bit-groups step) keeps binary + bit
    // groups, where the real packing happens.
    private fun List<Step>.simplifyBmp(): List<Step> = if (none { it is Step.BitGroups }) filterNot { it is Step.Binary } else this

    // Supplementary encode gets an explicit "subtract 0x10000" step (entered in
    // hex) right after the format choice, so the learner computes the 20-bit
    // value once and reads it back, instead of folding the subtraction into the
    // binary step. BMP (< 0x10000) has no offset, so it's left untouched.
    private fun List<Step>.withOffsetStep(codePointValue: Int): List<Step> {
        if (codePointValue < SUPPLEMENTARY_OFFSET) return this
        val afterFormat = indexOfFirst { it is Step.Format } + 1
        return toMutableList().apply {
            add(afterFormat, Step.Offset(expected = codePointValue - SUPPLEMENTARY_OFFSET))
        }
    }

    // Decode mirror: once the 20-bit scalar is assembled (binary step), an offset
    // step holds that value in hex, and the code-point step adds 0x10000. So the
    // "+ 0x10000" is explicit instead of folded into the code-point step.
    // Supplementary only; BMP has no binary step to follow.
    private fun List<Step>.withDecodeOffsetStep(codePointValue: Int): List<Step> {
        if (codePointValue < SUPPLEMENTARY_OFFSET) return this
        val afterBinary = indexOfFirst { it is Step.Binary } + 1
        return toMutableList().apply {
            add(afterBinary, Step.Offset(expected = codePointValue - SUPPLEMENTARY_OFFSET))
        }
    }

    private fun parseLevel(level: Int): Utf16Level = Utf16Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = encoding,
            level = level,
            reason = "level must be one of: ${Utf16Level.validNumbers}",
        )

    private companion object {
        private const val SUPPLEMENTARY_OFFSET = 0x10000
    }
}
