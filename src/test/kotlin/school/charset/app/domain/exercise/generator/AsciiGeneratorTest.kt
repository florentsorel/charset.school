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
import school.charset.app.domain.exercise.Step

class AsciiGeneratorTest :
    FreeSpec({
        val codec = Codec()

        fun newEncodeSut(codePoint: CodePoint, level: Int = 1): AsciiGenerator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val asciiLevel = AsciiLevel.fromNumber(level)!!
            every { codePointGenerator.randomAscii(asciiLevel) } returns codePoint
            return AsciiGenerator(codec, codePointGenerator, mockk())
        }

        fun newDecodeSut(bytes: ByteArray, level: Int = 1): AsciiGenerator {
            val byteArrayGenerator = mockk<ByteArrayGenerator>()
            val asciiLevel = AsciiLevel.fromNumber(level)!!
            every { byteArrayGenerator.randomAscii(asciiLevel) } returns bytes
            return AsciiGenerator(codec, mockk(), byteArrayGenerator)
        }

        "generateEncode" - {
            "builds [Binary(8), HexBytes(1)] with consistent values" {
                val sut = newEncodeSut(CodePoint(0x41))

                val exercise = sut.generateEncode(level = 1)

                exercise.encoding shouldBe Encoding.Ascii
                exercise.level shouldBe 1
                exercise.codePoint shouldBe CodePoint(0x41)
                exercise.steps shouldHaveSize 2

                val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
                val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()

                binary.length shouldBe 8
                binary.expected shouldBe "01000001"
                hex.expected shouldBe listOf(0x41)
            }

            "at low boundary U+0000 produces 00000000" {
                val sut = newEncodeSut(CodePoint(0x00), level = 2)
                val exercise = sut.generateEncode(level = 2)
                val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
                binary.expected shouldBe "00000000"
            }

            "at high boundary U+007F produces 01111111" {
                val sut = newEncodeSut(CodePoint(0x7F), level = 2)
                val exercise = sut.generateEncode(level = 2)
                val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
                binary.expected shouldBe "01111111"
            }

            "invalid level throws ExerciseGenerationException" {
                val sut = AsciiGenerator(codec, mockk(), mockk())
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generateEncode(level = 99)
                }
                exception.encoding shouldBe Encoding.Ascii
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for ascii level 99: level must be one of: 1, 2"
            }
        }

        "generateDecode" - {
            "builds [Binary(8), CodePointEntry] with consistent values" {
                // Input bytes [0x41] -> decode to U+0041 (A)
                val sut = newDecodeSut(byteArrayOf(0x41))

                val exercise = sut.generateDecode(level = 1)

                exercise.encoding shouldBe Encoding.Ascii
                exercise.level shouldBe 1
                exercise.bytes shouldBe byteArrayOf(0x41)
                exercise.steps shouldHaveSize 2

                val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
                val cpEntry = exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>()

                binary.length shouldBe 8
                binary.expected shouldBe "01000001"
                cpEntry.expected shouldBe 0x41
            }

            "at low boundary [0x00] -> U+0000 (NUL)" {
                val sut = newDecodeSut(byteArrayOf(0x00), level = 2)
                val exercise = sut.generateDecode(level = 2)
                exercise.steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "00000000"
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x00
            }

            "at high boundary [0x7F] -> U+007F (DEL)" {
                val sut = newDecodeSut(byteArrayOf(0x7F), level = 2)
                val exercise = sut.generateDecode(level = 2)
                exercise.steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "01111111"
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x7F
            }

            "invalid level throws ExerciseGenerationException" {
                val sut = AsciiGenerator(codec, mockk(), mockk())
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generateDecode(level = 99)
                }
                exception.encoding shouldBe Encoding.Ascii
                exception.level shouldBe 99
            }
        }
    })
