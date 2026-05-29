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

class Windows1252GeneratorTest :
    FreeSpec({
        val codec = Codec()

        fun newSut(codePoint: CodePoint, level: Int = 1): Windows1252Generator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val windows1252Level = Windows1252Level.fromNumber(level)!!
            every { codePointGenerator.randomWindows1252(windows1252Level) } returns codePoint
            return Windows1252Generator(codec, codePointGenerator, mockk())
        }

        "encoding is Windows1252" {
            val sut = Windows1252Generator(codec, mockk(), mockk())
            sut.encoding shouldBe Encoding.Windows1252
        }

        "Euro U+20AC produces byte 0x80 = 10000000" {
            val sut = newSut(CodePoint(0x20AC))

            val exercise = sut.generateEncode(level = 1)

            exercise.encoding shouldBe Encoding.Windows1252
            exercise.level shouldBe 1
            exercise.codePoint shouldBe CodePoint(0x20AC)
            exercise.steps shouldHaveSize 2

            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()

            binary.length shouldBe 8
            binary.expected shouldBe "10000000"
            hex.expected shouldBe listOf(0x80)
        }

        "Œ U+0152 produces byte 0x8C = 10001100" {
            val sut = newSut(CodePoint(0x0152))
            val exercise = sut.generateEncode(level = 1)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "10001100"
            hex.expected shouldBe listOf(0x8C)
        }

        "Ÿ U+0178 produces byte 0x9F = 10011111" {
            val sut = newSut(CodePoint(0x0178))
            val exercise = sut.generateEncode(level = 1)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "10011111"
            hex.expected shouldBe listOf(0x9F)
        }

        "ASCII identity U+0041 (A) produces byte 0x41 = 01000001 (level 2)" {
            val sut = newSut(CodePoint(0x41), level = 2)
            val exercise = sut.generateEncode(level = 2)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "01000001"
            hex.expected shouldBe listOf(0x41)
        }

        "Latin-1 identity U+00E9 (é) produces byte 0xE9 = 11101001 (level 2)" {
            val sut = newSut(CodePoint(0xE9), level = 2)
            val exercise = sut.generateEncode(level = 2)
            val binary = exercise.steps[0].shouldBeInstanceOf<Step.Binary>()
            val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()
            binary.expected shouldBe "11101001"
            hex.expected shouldBe listOf(0xE9)
        }

        "invalid level (encode) throws ExerciseGenerationException" {
            val sut = Windows1252Generator(codec, mockk(), mockk())
            val exception = shouldThrow<ExerciseGenerationException> {
                sut.generateEncode(level = 99)
            }
            exception.encoding shouldBe Encoding.Windows1252
            exception.level shouldBe 99
            exception.message shouldBe "Cannot generate exercise for windows-1252 level 99: level must be one of: 1, 2"
        }

        "buildEncodeStepsFor (sandbox)" - {
            val sut = Windows1252Generator(codec, mockk(), mockk())

            "ASCII identity U+0041 (A) -> byte 0x41" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x41))
                steps shouldHaveSize 2
                val binary = steps[0].shouldBeInstanceOf<Step.Binary>()
                val hex = steps[1].shouldBeInstanceOf<Step.HexBytes>()
                binary.expected shouldBe "01000001"
                binary.length shouldBe 8
                hex.expected shouldBe listOf(0x41)
            }

            "Latin-1 identity U+00E9 (e acute) -> byte 0xE9" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0xE9))
                val binary = steps[0].shouldBeInstanceOf<Step.Binary>()
                val hex = steps[1].shouldBeInstanceOf<Step.HexBytes>()
                binary.expected shouldBe "11101001"
                hex.expected shouldBe listOf(0xE9)
            }

            "Microsoft extension U+20AC (Euro) -> byte 0x80" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x20AC))
                val binary = steps[0].shouldBeInstanceOf<Step.Binary>()
                val hex = steps[1].shouldBeInstanceOf<Step.HexBytes>()
                binary.expected shouldBe "10000000"
                hex.expected shouldBe listOf(0x80)
            }

            "Microsoft extension U+2014 (em dash) -> byte 0x97" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x2014))
                val hex = steps[1].shouldBeInstanceOf<Step.HexBytes>()
                hex.expected shouldBe listOf(0x97)
            }

            "Microsoft extension U+2122 (trademark) -> byte 0x99" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x2122))
                val hex = steps[1].shouldBeInstanceOf<Step.HexBytes>()
                hex.expected shouldBe listOf(0x99)
            }

            "Microsoft extension U+2018 (left single quote, smart quote) -> byte 0x91" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x2018))
                val hex = steps[1].shouldBeInstanceOf<Step.HexBytes>()
                hex.expected shouldBe listOf(0x91)
            }

            "U+0100 (Latin A with macron) is rejected as not encodable" {
                shouldThrow<school.charset.app.domain.encoding.EncoderException> {
                    sut.buildEncodeStepsFor(CodePoint(0x0100))
                }
            }
        }

        "buildDecodeStepsFor (sandbox)" - {
            val sut = Windows1252Generator(codec, mockk(), mockk())

            "ASCII identity byte 0x41 -> U+0041 (A)" {
                val bytes = byteArrayOf(0x41)
                val cp = codec.decode(bytes, Encoding.Windows1252)
                val steps = sut.buildDecodeStepsFor(bytes, cp)
                steps shouldHaveSize 2
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "01000001"
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x41
            }

            "Latin-1 identity byte 0xE9 -> U+00E9 (e acute)" {
                val bytes = byteArrayOf(0xE9.toByte())
                val cp = codec.decode(bytes, Encoding.Windows1252)
                val steps = sut.buildDecodeStepsFor(bytes, cp)
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "11101001"
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }

            "Microsoft extension byte 0x80 -> U+20AC (Euro)" {
                val bytes = byteArrayOf(0x80.toByte())
                val cp = codec.decode(bytes, Encoding.Windows1252)
                val steps = sut.buildDecodeStepsFor(bytes, cp)
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "10000000"
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x20AC
            }

            "Microsoft extension byte 0x97 -> U+2014 (em dash)" {
                val bytes = byteArrayOf(0x97.toByte())
                val cp = codec.decode(bytes, Encoding.Windows1252)
                val steps = sut.buildDecodeStepsFor(bytes, cp)
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x2014
            }

            "unassigned byte 0x81 is rejected by Codec.decode" {
                shouldThrow<school.charset.app.domain.encoding.DecoderException> {
                    codec.decode(byteArrayOf(0x81.toByte()), Encoding.Windows1252)
                }
            }

            "unassigned byte 0x8D is rejected by Codec.decode" {
                shouldThrow<school.charset.app.domain.encoding.DecoderException> {
                    codec.decode(byteArrayOf(0x8D.toByte()), Encoding.Windows1252)
                }
            }

            "unassigned byte 0x8F is rejected by Codec.decode" {
                shouldThrow<school.charset.app.domain.encoding.DecoderException> {
                    codec.decode(byteArrayOf(0x8F.toByte()), Encoding.Windows1252)
                }
            }

            "unassigned byte 0x90 is rejected by Codec.decode" {
                shouldThrow<school.charset.app.domain.encoding.DecoderException> {
                    codec.decode(byteArrayOf(0x90.toByte()), Encoding.Windows1252)
                }
            }

            "unassigned byte 0x9D is rejected by Codec.decode" {
                shouldThrow<school.charset.app.domain.encoding.DecoderException> {
                    codec.decode(byteArrayOf(0x9D.toByte()), Encoding.Windows1252)
                }
            }
        }

        "generateDecode" - {
            fun newDecodeSut(bytes: ByteArray, level: Int = 1): Windows1252Generator {
                val bag = mockk<ByteArrayGenerator>()
                val win1252Level = Windows1252Level.fromNumber(level)!!
                every { bag.randomWindows1252(win1252Level) } returns bytes
                return Windows1252Generator(codec, mockk(), bag)
            }

            "[0x80] (Euro special block) -> CodePointEntry=U+20AC" {
                val sut = newDecodeSut(byteArrayOf(0x80.toByte()))
                val exercise = sut.generateDecode(level = 1)

                exercise.encoding shouldBe Encoding.Windows1252
                exercise.bytes shouldBe byteArrayOf(0x80.toByte())
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "10000000"
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x20AC
            }

            "[0x9F] (Ÿ, last special) -> CodePointEntry=U+0178" {
                val sut = newDecodeSut(byteArrayOf(0x9F.toByte()))
                val exercise = sut.generateDecode(level = 1)
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x0178
            }

            "[0x41] (ASCII identity, level 2) -> CodePointEntry=0x41" {
                val sut = newDecodeSut(byteArrayOf(0x41), level = 2)
                val exercise = sut.generateDecode(level = 2)
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x41
            }

            "[0xE9] (Latin-1 identity, level 2) -> CodePointEntry=0xE9" {
                val sut = newDecodeSut(byteArrayOf(0xE9.toByte()), level = 2)
                val exercise = sut.generateDecode(level = 2)
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }
        }
    })
