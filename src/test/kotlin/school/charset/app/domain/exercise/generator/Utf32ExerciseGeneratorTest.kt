package school.charset.app.domain.exercise.generator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Step

class Utf32ExerciseGeneratorTest :
    FreeSpec({
        val codec = Codec()
        val utf32Generator = Utf32Generator(codec)
        val codePointGenerator = mockk<CodePointGenerator>()
        val byteArrayGenerator = mockk<ByteArrayGenerator>()

        val generator = Utf32ExerciseGenerator(
            codec = codec,
            encoding = Encoding.Utf32Be,
            codePointGenerator = codePointGenerator,
            byteArrayGenerator = byteArrayGenerator,
            utf32Generator = utf32Generator,
        )

        fun List<Step>.types(): List<String> = map { it::class.simpleName.orEmpty() }

        "the exercise never asks the learner to pick the endianness (it's given in the header)" {
            every { codePointGenerator.randomUtf32(Utf32Level.Bmp) } returns CodePoint(0xE9)
            every { codePointGenerator.randomUtf32(Utf32Level.Supplementary) } returns CodePoint(0x1F389)

            generator.generateEncode(1).steps.none { it is Step.Endianness } shouldBe true
            generator.generateEncode(2).steps.none { it is Step.Endianness } shouldBe true
        }

        "encode is binary (32-bit) then hex bytes - same shape for BMP and supplementary" {
            every { codePointGenerator.randomUtf32(Utf32Level.Bmp) } returns CodePoint(0xE9)

            val steps = generator.generateEncode(1).steps

            steps.types() shouldContainExactly listOf("Binary", "HexBytes")
            (steps[0] as Step.Binary).length shouldBe 32
            (steps[1] as Step.HexBytes).expected.shouldContainExactly(0x00, 0x00, 0x00, 0xE9)
        }

        "decode is binary (32-bit) then code point" {
            every { byteArrayGenerator.randomUtf32(Utf32Level.Bmp, Encoding.Utf32Be) } returns
                byteArrayOf(0x00, 0x00, 0x00, 0xE9.toByte())

            val steps = generator.generateDecode(1).steps

            steps.types() shouldContainExactly listOf("Binary", "CodePointEntry")
            (steps[0] as Step.Binary).length shouldBe 32
            (steps[1] as Step.CodePointEntry).expected shouldBe 0xE9
        }
    })
