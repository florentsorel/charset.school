package school.charset.app.domain.exercise.generator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class AsciiGeneratorTest :
    FreeSpec({
        val codec = Codec()

        fun newSut(codePoint: CodePoint, level: Int = 1): AsciiGenerator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val asciiLevel = AsciiLevel.fromNumber(level)!!
            every { codePointGenerator.randomAscii(asciiLevel) } returns codePoint
            return AsciiGenerator(codec, codePointGenerator)
        }

        "verbose builds [Binary(8), HexBytes(1)] with consistent values" {
            val sut = newSut(CodePoint(0x41))

            val exercise = sut.generate(level = 1, Granularity.Verbose)

            exercise.encoding shouldBe Encoding.Ascii
            exercise.level shouldBe 1
            exercise.granularity shouldBe Granularity.Verbose
            exercise.codePoint shouldBe CodePoint(0x41)
            exercise.steps shouldHaveSize 2

            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()

            binary.length shouldBe 8
            binary.expected shouldBe "01000001"
            hex.expected shouldBe listOf(0x41)
        }

        "verbose at low boundary U+0000 produces 00000000" {
            val sut = newSut(CodePoint(0x00), level = 2)
            val exercise = sut.generate(level = 2, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            binary.expected shouldBe "00000000"
        }

        "verbose at high boundary U+007F produces 01111111" {
            val sut = newSut(CodePoint(0x7F), level = 2)
            val exercise = sut.generate(level = 2, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            binary.expected shouldBe "01111111"
        }

        "standard builds [HexBytes(1)] only" {
            val sut = newSut(CodePoint(0x41))
            val exercise = sut.generate(level = 1, Granularity.Standard)

            exercise.steps shouldHaveSize 1
            exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x41)
        }

        "compact builds [HexBytes(1)] only" {
            val sut = newSut(CodePoint(0x41))
            val exercise = sut.generate(level = 1, Granularity.Compact)

            exercise.steps shouldHaveSize 1
            exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x41)
        }

        "invalid level throws ExerciseGenerationException" {
            val sut = AsciiGenerator(codec, mockk())
            val exception = shouldThrow<ExerciseGenerationException> {
                sut.generate(level = 99, Granularity.Verbose)
            }
            exception.encoding shouldBe Encoding.Ascii
            exception.level shouldBe 99
            exception.message shouldBe "Cannot generate exercise for ascii level 99: level must be one of: 1, 2"
        }
    })
