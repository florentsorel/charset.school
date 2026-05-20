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

class Latin1GeneratorTest :
    FreeSpec({
        val codec = Codec()

        fun newSut(codePoint: CodePoint, level: Int = 1): Latin1Generator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val latin1Level = Latin1Level.fromNumber(level)!!
            every { codePointGenerator.randomLatin1(latin1Level) } returns codePoint
            return Latin1Generator(codec, codePointGenerator)
        }

        "encoding is Latin1" {
            val sut = Latin1Generator(codec, mockk())
            sut.encoding shouldBe Encoding.Latin1
        }

        "verbose builds [Binary(8), HexBytes(1)] with consistent values for U+00E9 (é)" {
            val sut = newSut(CodePoint(0xE9))

            val exercise = sut.generate(level = 1, Granularity.Verbose)

            exercise.encoding shouldBe Encoding.Latin1
            exercise.level shouldBe 1
            exercise.granularity shouldBe Granularity.Verbose
            exercise.codePoint shouldBe CodePoint(0xE9)
            exercise.steps shouldHaveSize 2

            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()

            binary.length shouldBe 8
            binary.expected shouldBe "11101001"
            hex.expected shouldBe listOf(0xE9)
        }

        "verbose at Latin-1 supplement low boundary U+00A0 produces 10100000" {
            val sut = newSut(CodePoint(0xA0))
            val exercise = sut.generate(level = 1, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            binary.expected shouldBe "10100000"
        }

        "verbose at high boundary U+00FF (ÿ) produces 11111111" {
            val sut = newSut(CodePoint(0xFF))
            val exercise = sut.generate(level = 1, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            binary.expected shouldBe "11111111"
        }

        "verbose at low boundary U+0000 produces 00000000 (level 2)" {
            val sut = newSut(CodePoint(0x00), level = 2)
            val exercise = sut.generate(level = 2, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            binary.expected shouldBe "00000000"
        }

        "standard builds [HexBytes(1)] only" {
            val sut = newSut(CodePoint(0xE9))
            val exercise = sut.generate(level = 1, Granularity.Standard)

            exercise.steps shouldHaveSize 1
            exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE9)
        }

        "compact builds [HexBytes(1)] only" {
            val sut = newSut(CodePoint(0xE9))
            val exercise = sut.generate(level = 1, Granularity.Compact)

            exercise.steps shouldHaveSize 1
            exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE9)
        }

        "invalid level throws ExerciseGenerationException" {
            val sut = Latin1Generator(codec, mockk())
            val exception = shouldThrow<ExerciseGenerationException> {
                sut.generate(level = 99, Granularity.Verbose)
            }
            exception.encoding shouldBe Encoding.Latin1
            exception.level shouldBe 99
            exception.message shouldBe "Cannot generate exercise for latin1 level 99: level must be one of: 1, 2"
        }
    })
