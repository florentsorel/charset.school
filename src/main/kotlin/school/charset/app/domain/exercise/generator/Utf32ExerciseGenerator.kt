package school.charset.app.domain.exercise.generator

import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException

class Utf32ExerciseGenerator(
    private val codec: Codec,
    override val encoding: Encoding,
    private val codePointGenerator: CodePointGenerator,
    private val byteArrayGenerator: ByteArrayGenerator,
    private val utf32Generator: Utf32Generator,
) : EncodingExerciseGenerator {

    init {
        require(encoding == Encoding.Utf32Be || encoding == Encoding.Utf32Le) {
            "Utf32ExerciseGenerator requires Utf32Be or Utf32Le, got ${encoding.id}"
        }
    }

    private val endian: Encoding.Endian =
        if (encoding == Encoding.Utf32Be) Encoding.Endian.BigEndian else Encoding.Endian.LittleEndian

    override fun generateEncode(level: Int): Exercise.Encode {
        val utf32Level = parseLevel(level)
        val codePoint = codePointGenerator.randomUtf32(utf32Level)
        val steps = utf32Generator.buildEncodeStepsFor(codePoint, endian)
        return Exercise.Encode(codePoint, encoding, level, steps)
    }

    override fun generateDecode(level: Int): Exercise.Decode {
        val utf32Level = parseLevel(level)
        val bytes = byteArrayGenerator.randomUtf32(utf32Level, encoding)
        val codePoint = codec.decode(bytes, encoding)
        val steps = utf32Generator.buildDecodeStepsFor(bytes, codePoint, endian)
        return Exercise.Decode(bytes, codePoint, encoding, level, steps)
    }

    private fun parseLevel(level: Int): Utf32Level = Utf32Level.fromNumber(level)
        ?: throw ExerciseGenerationException(
            encoding = encoding,
            level = level,
            reason = "level must be one of: ${Utf32Level.validNumbers}",
        )
}
