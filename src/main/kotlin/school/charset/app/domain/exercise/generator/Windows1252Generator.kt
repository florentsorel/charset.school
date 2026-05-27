package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Windows1252Generator(
    private val codec: Codec,
    private val codePointGenerator: CodePointGenerator,
    private val byteArrayGenerator: ByteArrayGenerator,
) : EncodingExerciseGenerator {
    override val encoding = Encoding.Windows1252

    override fun generateEncode(level: Int, granularity: Granularity): Exercise.Encode {
        val windows1252Level = parseLevel(level)
        val codePoint = codePointGenerator.randomWindows1252(windows1252Level)
        val steps = codePoint.buildEncodeSteps(granularity)
        return Exercise.Encode(codePoint, Encoding.Windows1252, level, granularity, steps)
    }

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val windows1252Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomWindows1252(windows1252Level)
        val codePoint = codec.decode(bytes, Encoding.Windows1252)
        val steps = bytes.buildDecodeSteps(codePoint, granularity)
        return Exercise.Decode(bytes, Encoding.Windows1252, level, granularity, steps)
    }

    /**
     * Sandbox helper: build the pedagogical steps for encoding [codePoint] in
     * Windows-1252 at the given [granularity], without going through the
     * level-based exercise flow. Used by `SandboxService.encodeWindows1252Verbose`.
     *
     * Re-uses the encode pipeline of the level-based generator, so any change
     * to the step layout (e.g. number of bits, hex byte count) is shared.
     */
    fun buildEncodeStepsFor(codePoint: CodePoint, granularity: Granularity): List<Step> = codePoint.buildEncodeSteps(granularity)

    /**
     * Sandbox helper: build the pedagogical steps for decoding [bytes] in
     * Windows-1252 at the given [granularity]. Takes the already-decoded
     * [codePoint] as input (the caller decodes via `Codec.decode` so we
     * don't double-decode here, and so the caller sees the `DecoderException`
     * for unassigned bytes early enough to surface it as an HTTP 422).
     */
    fun buildDecodeStepsFor(bytes: ByteArray, codePoint: CodePoint, granularity: Granularity): List<Step> = bytes.buildDecodeSteps(codePoint, granularity)

    private fun parseLevel(level: Int): Windows1252Level = Windows1252Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = Encoding.Windows1252,
            level = level,
            reason = "level must be one of: ${Windows1252Level.validNumbers}",
        )

    private fun CodePoint.buildEncodeSteps(granularity: Granularity): List<Step> {
        val bytes = codec.encode(this, Encoding.Windows1252)
        val byte = bytes[0].toInt() and 0xFF
        val binary = byte.toString(2).padStart(8, '0')

        return when (granularity) {
            Granularity.Verbose -> listOf(
                Step.Binary(expected = binary, length = 8),
                Step.HexBytes(expected = listOf(byte)),
            )

            Granularity.Standard,
            Granularity.Compact,
            -> listOf(
                Step.HexBytes(expected = listOf(byte)),
            )
        }
    }

    private fun ByteArray.buildDecodeSteps(codePoint: CodePoint, granularity: Granularity): List<Step> {
        // Windows-1252 decode is identity on 0x00..0x7F and 0xA0..0xFF, with a
        // table lookup for 0x80..0x9F (the 27 special chars: €, Œ, ™, etc.).
        // The caller decodes via Codec.decode so unassigned bytes (0x81, 0x8D,
        // 0x8F, 0x90, 0x9D) raise a DecoderException early - the step builder
        // only sees decodable inputs here.
        val byte = this[0].toInt() and 0xFF
        val binary = byte.toString(2).padStart(8, '0')

        return when (granularity) {
            Granularity.Verbose -> listOf(
                Step.Binary(expected = binary, length = 8),
                Step.CodePointEntry(expected = codePoint.value),
            )

            Granularity.Standard,
            Granularity.Compact,
            -> listOf(
                Step.CodePointEntry(expected = codePoint.value),
            )
        }
    }
}
