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
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf32GeneratorTest :
    FreeSpec({
        val codec = Codec()
        val formatChoices = listOf(
            FormatChoice.ONE_BYTE,
            FormatChoice.TWO_BYTES,
            FormatChoice.THREE_BYTES,
            FormatChoice.FOUR_BYTES,
        )

        fun newSut(encoding: Encoding, codePoint: CodePoint, level: Int): Utf32Generator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val utf32Level = Utf32Level.fromNumber(level)!!
            every { codePointGenerator.randomUtf32(utf32Level) } returns codePoint
            return Utf32Generator(codec, codePointGenerator, mockk(), encoding)
        }

        "constructor" - {
            "encoding = Utf32Be is accepted" {
                Utf32Generator(codec, mockk(), mockk(), Encoding.Utf32Be).encoding shouldBe Encoding.Utf32Be
            }

            "encoding = Utf32Le is accepted" {
                Utf32Generator(codec, mockk(), mockk(), Encoding.Utf32Le).encoding shouldBe Encoding.Utf32Le
            }

            "throws if encoding is not a UTF-32 variant" {
                val exception = shouldThrow<IllegalArgumentException> {
                    Utf32Generator(codec, mockk(), mockk(), Encoding.Utf8)
                }
                exception.message shouldBe
                    "Utf32Generator handles only utf-32be, utf-32le, got utf-8"
            }
        }

        // Structural assertions identical between BE and LE (Format, Binary, BitGroups
        // are endian-agnostic - only HexBytes differs).
        "structural / verbose BMP - U+00E9 (é)" - {
            "Format=4 bytes, Binary(32)=...11101001, BitGroups=4×8, steps count=4" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val bitGroups = exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>()

                format.choices shouldBe formatChoices
                format.expected shouldBe FormatChoice.FOUR_BYTES
                binary.length shouldBe 32
                binary.expected shouldBe "00000000000000000000000011101001"
                bitGroups.expected shouldBe listOf("00000000", "00000000", "00000000", "11101001")
            }
        }

        "structural / verbose Supplementary - U+1F600 (😀)" - {
            "Binary(32)=...11111011000000000, BitGroups split exact" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x1F600), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val bitGroups = exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>()

                binary.length shouldBe 32
                binary.expected shouldBe "00000000000000011111011000000000"
                bitGroups.expected shouldBe listOf("00000000", "00000001", "11110110", "00000000")
            }
        }

        // Byte-order-specific: HexBytes content differs between BE and LE.
        "Be / verbose BMP" - {
            "U+0000 (low boundary) -> bytes [0x00, 0x00, 0x00, 0x00]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x00), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x00, 0x00)
            }

            "U+0041 (A) -> bytes [0x00, 0x00, 0x00, 0x41]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x41), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x00, 0x41)
            }

            "U+00E9 (é) -> bytes [0x00, 0x00, 0x00, 0xE9]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x00, 0xE9)
            }

            "U+4E2D (中) -> bytes [0x00, 0x00, 0x4E, 0x2D]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x4E2D), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x4E, 0x2D)
            }

            "U+FFFF (BMP max) -> bytes [0x00, 0x00, 0xFF, 0xFF]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0xFFFF), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0xFF, 0xFF)
            }
        }

        "Be / verbose Supplementary" - {
            "U+10000 (low boundary) -> bytes [0x00, 0x01, 0x00, 0x00]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x10000), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x01, 0x00, 0x00)
            }

            "U+1F600 (😀) -> bytes [0x00, 0x01, 0xF6, 0x00]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x1F600), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x01, 0xF6, 0x00)
            }

            "U+10FFFF (high boundary) -> bytes [0x00, 0x10, 0xFF, 0xFF]" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0x10FFFF), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x10, 0xFF, 0xFF)
            }
        }

        "Le / verbose BMP" - {
            "U+0000 (palindromic) -> bytes [0x00, 0x00, 0x00, 0x00]" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x00), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x00, 0x00)
            }

            "U+0041 (A) -> bytes [0x41, 0x00, 0x00, 0x00] (reversed vs BE)" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x41), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x41, 0x00, 0x00, 0x00)
            }

            "U+4E2D (中) -> bytes [0x2D, 0x4E, 0x00, 0x00] (reversed vs BE)" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x4E2D), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x2D, 0x4E, 0x00, 0x00)
            }

            "U+FFFF (BMP max) -> bytes [0xFF, 0xFF, 0x00, 0x00]" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0xFFFF), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0xFF, 0xFF, 0x00, 0x00)
            }
        }

        "Le / verbose Supplementary" - {
            "U+10000 (low boundary) -> bytes [0x00, 0x00, 0x01, 0x00]" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x10000), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x01, 0x00)
            }

            "U+1F600 (😀) -> bytes [0x00, 0xF6, 0x01, 0x00]" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x1F600), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0xF6, 0x01, 0x00)
            }

            "U+10FFFF (high boundary) -> bytes [0xFF, 0xFF, 0x10, 0x00]" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x10FFFF), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0xFF, 0xFF, 0x10, 0x00)
            }
        }

        "standard / produces [Format, HexBytes]" - {
            "Be BMP" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.FOUR_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x00, 0xE9)
            }

            "Le Supplementary" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x1F600), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0xF6, 0x01, 0x00)
            }
        }

        "compact / produces [HexBytes] only" - {
            "Be BMP" {
                val sut = newSut(Encoding.Utf32Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0x00, 0x00, 0xE9)
            }

            "Le Supplementary" {
                val sut = newSut(Encoding.Utf32Le, CodePoint(0x1F600), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0xF6, 0x01, 0x00)
            }
        }

        "invalid level (encode) throws ExerciseGenerationException" - {
            "Be" {
                val sut = Utf32Generator(codec, mockk(), mockk(), Encoding.Utf32Be)
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generateEncode(level = 99, Granularity.Verbose)
                }
                exception.encoding shouldBe Encoding.Utf32Be
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for utf-32be level 99: level must be one of: 1, 2"
            }

            "Le" {
                val sut = Utf32Generator(codec, mockk(), mockk(), Encoding.Utf32Le)
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generateEncode(level = 99, Granularity.Verbose)
                }
                exception.encoding shouldBe Encoding.Utf32Le
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for utf-32le level 99: level must be one of: 1, 2"
            }
        }

        "generateDecode" - {
            fun newDecodeSut(encoding: Encoding, bytes: ByteArray, level: Int): Utf32Generator {
                val bag = mockk<ByteArrayGenerator>()
                val utf32Level = Utf32Level.fromNumber(level)!!
                every { bag.randomUtf32(utf32Level, encoding) } returns bytes
                return Utf32Generator(codec, mockk(), bag, encoding)
            }

            "Be / verbose BMP [00, 00, 00, 41] (A) -> Format + BitGroups + Binary(32) + CodePointEntry" {
                val sut = newDecodeSut(Encoding.Utf32Be, byteArrayOf(0x00, 0x00, 0x00, 0x41), level = 1)
                val exercise = sut.generateDecode(level = 1, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.FOUR_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("00000000", "00000000", "00000000", "01000001")
                exercise.steps[2].shouldBeInstanceOf<Step.Binary>().expected shouldBe
                    "00000000000000000000000001000001"
                exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x41
            }

            "Le / verbose BMP [41, 00, 00, 00] (A, fully reversed) -> same binary as BE" {
                val sut = newDecodeSut(Encoding.Utf32Le, byteArrayOf(0x41, 0x00, 0x00, 0x00), level = 1)
                val exercise = sut.generateDecode(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.Binary>().expected shouldBe
                    "00000000000000000000000001000001"
                exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x41
            }

            "Be / verbose Supplementary [00, 01, F6, 00] (😀) -> CodePointEntry=0x1F600" {
                val sut = newDecodeSut(
                    Encoding.Utf32Be,
                    byteArrayOf(0x00, 0x01, 0xF6.toByte(), 0x00),
                    level = 2,
                )
                val exercise = sut.generateDecode(level = 2, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("00000000", "00000001", "11110110", "00000000")
                exercise.steps[2].shouldBeInstanceOf<Step.Binary>().expected shouldBe
                    "00000000000000011111011000000000"
                exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x1F600
            }

            "Le / verbose Supplementary [00, F6, 01, 00] (😀) -> same binary as BE" {
                val sut = newDecodeSut(
                    Encoding.Utf32Le,
                    byteArrayOf(0x00, 0xF6.toByte(), 0x01, 0x00),
                    level = 2,
                )
                val exercise = sut.generateDecode(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x1F600
            }

            "standard builds [Format, CodePointEntry]" {
                val sut = newDecodeSut(Encoding.Utf32Be, byteArrayOf(0x00, 0x00, 0x00, 0xE9.toByte()), level = 1)
                val exercise = sut.generateDecode(level = 1, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }

            "compact builds [CodePointEntry] only" {
                val sut = newDecodeSut(Encoding.Utf32Le, byteArrayOf(0xE9.toByte(), 0x00, 0x00, 0x00), level = 1)
                val exercise = sut.generateDecode(level = 1, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }
        }
    })
