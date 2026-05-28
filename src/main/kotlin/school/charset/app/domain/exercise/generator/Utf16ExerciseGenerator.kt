package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity

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

    override fun generateEncode(level: Int, granularity: Granularity): Exercise.Encode {
        val utf16Level = parseLevel(level)
        val codePoint = codePointGenerator.randomUtf16(utf16Level)
        val steps = utf16Generator.buildEncodeStepsFor(codePoint, endian, granularity)
        return Exercise.Encode(codePoint, encoding, level, granularity, steps)
    }

    override fun generateDecode(level: Int, granularity: Granularity): Exercise.Decode {
        val utf16Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf16(utf16Level, encoding)
        val codePoint = codec.decode(bytes, encoding)
        val steps = utf16Generator.buildDecodeStepsFor(bytes, codePoint, endian, granularity)
        return Exercise.Decode(bytes, codePoint, encoding, level, granularity, steps)
    }

    private fun parseLevel(level: Int): Utf16Level = Utf16Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = encoding,
            level = level,
            reason = "level must be one of: ${Utf16Level.validNumbers}",
        )
}
