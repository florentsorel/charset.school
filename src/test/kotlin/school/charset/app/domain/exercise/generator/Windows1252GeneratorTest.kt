package school.charset.app.domain.exercise.generator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Windows1252GeneratorTest :
    FreeSpec({
        val codec = Codec()

        fun newSut(codePoint: CodePoint, level: Int = 1): Windows1252Generator {
            val codePointGenerator = mockk<CodePointGenerator>()
            every { codePointGenerator.randomWindows1252(level) } returns codePoint
            return Windows1252Generator(codec, codePointGenerator)
        }

        "encoding is Windows1252" {
            val sut = Windows1252Generator(codec, mockk())
            sut.encoding shouldBe Encoding.Windows1252
        }

        "verbose with Euro U+20AC produces byte 0x80 = 10000000" {
            val sut = newSut(CodePoint(0x20AC))

            val exercise = sut.generate(level = 1, Granularity.Verbose)

            exercise.encoding shouldBe Encoding.Windows1252
            exercise.level shouldBe 1
            exercise.granularity shouldBe Granularity.Verbose
            exercise.codePoint shouldBe CodePoint(0x20AC)
            exercise.steps shouldHaveSize 2

            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()

            binary.length shouldBe 8
            binary.expected shouldBe "10000000"
            hex.expected shouldBe listOf(0x80)
        }

        "verbose with Œ U+0152 produces byte 0x8C = 10001100" {
            val sut = newSut(CodePoint(0x0152))
            val exercise = sut.generate(level = 1, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "10001100"
            hex.expected shouldBe listOf(0x8C)
        }

        "verbose with Ÿ U+0178 produces byte 0x9F = 10011111" {
            val sut = newSut(CodePoint(0x0178))
            val exercise = sut.generate(level = 1, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "10011111"
            hex.expected shouldBe listOf(0x9F)
        }

        "verbose with ASCII identity U+0041 (A) produces byte 0x41 = 01000001 (level 2)" {
            val sut = newSut(CodePoint(0x41), level = 2)
            val exercise = sut.generate(level = 2, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "01000001"
            hex.expected shouldBe listOf(0x41)
        }

        "verbose with Latin-1 identity U+00E9 (é) produces byte 0xE9 = 11101001 (level 2)" {
            val sut = newSut(CodePoint(0xE9), level = 2)
            val exercise = sut.generate(level = 2, Granularity.Verbose)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "11101001"
            hex.expected shouldBe listOf(0xE9)
        }

        "standard builds [HexBytes(1)] only" {
            val sut = newSut(CodePoint(0x20AC))
            val exercise = sut.generate(level = 1, Granularity.Standard)

            exercise.steps shouldHaveSize 1
            exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x80)
        }

        "compact builds [HexBytes(1)] only" {
            val sut = newSut(CodePoint(0x20AC))
            val exercise = sut.generate(level = 1, Granularity.Compact)

            exercise.steps shouldHaveSize 1
            exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x80)
        }
    })
