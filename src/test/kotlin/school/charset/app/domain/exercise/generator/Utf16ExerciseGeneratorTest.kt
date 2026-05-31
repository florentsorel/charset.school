package school.charset.app.domain.exercise.generator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Step

class Utf16ExerciseGeneratorTest :
    FreeSpec({
        val codec = Codec()
        val utf16Generator = Utf16Generator(codec)
        val codePointGenerator = mockk<CodePointGenerator>()
        val byteArrayGenerator = mockk<ByteArrayGenerator>()

        val generator = Utf16ExerciseGenerator(
            codec = codec,
            encoding = Encoding.Utf16Be,
            codePointGenerator = codePointGenerator,
            byteArrayGenerator = byteArrayGenerator,
            utf16Generator = utf16Generator,
        )

        fun List<Step>.types(): List<String> = map { it::class.simpleName.orEmpty() }

        "the exercise never asks the learner to pick the endianness (it's given in the header)" {
            every { codePointGenerator.randomUtf16(Utf16Level.Bmp) } returns CodePoint(0xE9)
            every { codePointGenerator.randomUtf16(Utf16Level.Supplementary) } returns CodePoint(0x1F389)

            generator.generateEncode(1).steps.none { it is Step.Endianness } shouldBe true
            generator.generateEncode(2).steps.none { it is Step.Endianness } shouldBe true
        }

        "encode" - {
            "BMP is a direct copy - no binary step, straight from format to hex bytes" {
                every { codePointGenerator.randomUtf16(Utf16Level.Bmp) } returns CodePoint(0xE9)

                val steps = generator.generateEncode(1).steps

                steps.types() shouldContainExactly listOf("Format", "HexBytes")
                (steps[0] as Step.Format).expected shouldBe FormatChoice.ONE_CODE_UNIT
                (steps[1] as Step.HexBytes).expected.shouldContainExactly(0x00, 0xE9)
            }

            "supplementary inserts an offset step, then keeps binary + bit-groups (surrogate pair)" {
                every { codePointGenerator.randomUtf16(Utf16Level.Supplementary) } returns CodePoint(0x1F389)

                val steps = generator.generateEncode(2).steps

                steps.types() shouldContainExactly listOf("Format", "Offset", "Binary", "BitGroups", "HexBytes")
                (steps[0] as Step.Format).expected shouldBe FormatChoice.TWO_CODE_UNITS
                // 0x1F389 - 0x10000 = 0xF389
                (steps[1] as Step.Offset).expected shouldBe 0xF389
            }
        }

        "decode" - {
            "BMP reads straight from format to code point - no binary step" {
                every { byteArrayGenerator.randomUtf16(Utf16Level.Bmp, Encoding.Utf16Be) } returns
                    byteArrayOf(0x00, 0xE9.toByte())

                val steps = generator.generateDecode(1).steps

                steps.types() shouldContainExactly listOf("Format", "CodePointEntry")
                (steps[1] as Step.CodePointEntry).expected shouldBe 0xE9
            }

            "supplementary inserts an offset step after binary, before the code point" {
                every { byteArrayGenerator.randomUtf16(Utf16Level.Supplementary, Encoding.Utf16Be) } returns
                    byteArrayOf(0xD8.toByte(), 0x3C, 0xDF.toByte(), 0x89.toByte())

                val steps = generator.generateDecode(2).steps

                steps.types() shouldContainExactly listOf("Format", "BitGroups", "Binary", "Offset", "CodePointEntry")
                // 0x1F389 - 0x10000 = 0xF389
                (steps[3] as Step.Offset).expected shouldBe 0xF389
                (steps[4] as Step.CodePointEntry).expected shouldBe 0x1F389
            }
        }
    })
