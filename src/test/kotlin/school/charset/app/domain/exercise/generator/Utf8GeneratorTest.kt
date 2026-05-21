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

class Utf8GeneratorTest :
    FreeSpec({
        val codec = Codec()
        val formatChoices = listOf(
            FormatChoice.ONE_BYTE,
            FormatChoice.TWO_BYTES,
            FormatChoice.THREE_BYTES,
            FormatChoice.FOUR_BYTES,
        )

        fun newSut(codePoint: CodePoint, level: Int): Utf8Generator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val utf8Level = Utf8Level.fromNumber(level)!!
            every { codePointGenerator.randomUtf8(utf8Level) } returns codePoint
            return Utf8Generator(codec, codePointGenerator, mockk())
        }

        "encoding is Utf8" {
            Utf8Generator(codec, mockk(), mockk()).encoding shouldBe Encoding.Utf8
        }

        "verbose / 1-byte" - {
            // U+0041 (A) -> 0x41, binary 1000001 (7 bits, no leading zero in MSB position 6)
            "U+0041 (A) -> Format + Binary(7) + HexBytes, NO BitGroups" {
                val sut = newSut(CodePoint(0x41), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)

                exercise.encoding shouldBe Encoding.Utf8
                exercise.codePoint shouldBe CodePoint(0x41)
                exercise.steps shouldHaveSize 3

                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val hex = exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>()

                format.choices shouldBe formatChoices
                format.expected shouldBe FormatChoice.ONE_BYTE
                binary.length shouldBe 7
                binary.expected shouldBe "1000001"
                hex.expected shouldBe listOf(0x41)
            }

            "U+0000 (NUL, low boundary) -> Binary(7) = 0000000, byte 0x00" {
                val sut = newSut(CodePoint(0x00), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0000000"
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00)
            }

            "U+007F (DEL, high boundary) -> Binary(7) = 1111111, byte 0x7F" {
                val sut = newSut(CodePoint(0x7F), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "1111111"
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x7F)
            }
        }

        "verbose / 2-byte" - {
            // U+00E9 (é) -> bytes C3 A9, binary 00011101001 (11 bits), split 00011 / 101001
            "U+00E9 (é, canary) -> Format + Binary(11) + BitGroups(5,6) + HexBytes" {
                val sut = newSut(CodePoint(0xE9), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)

                exercise.steps shouldHaveSize 4

                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val bitGroups = exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>()
                val hex = exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>()

                format.expected shouldBe FormatChoice.TWO_BYTES
                binary.length shouldBe 11
                binary.expected shouldBe "00011101001"
                bitGroups.expected shouldBe listOf("00011", "101001")
                hex.expected shouldBe listOf(0xC3, 0xA9)
            }

            "U+0080 (low boundary) -> BitGroups(00010, 000000), bytes C2 80" {
                val sut = newSut(CodePoint(0x80), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "00010000000"
                exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("00010", "000000")
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xC2, 0x80)
            }

            "U+07FF (high boundary) -> Binary 11111111111, BitGroups(11111, 111111), bytes DF BF" {
                val sut = newSut(CodePoint(0x7FF), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "11111111111"
                exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("11111", "111111")
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xDF, 0xBF)
            }
        }

        "verbose / 3-byte" - {
            // U+4E2D (中) -> bytes E4 B8 AD, binary 0100111000101101 (16 bits), split 0100/111000/101101
            "U+4E2D (中) -> Format + Binary(16) + BitGroups(4,6,6) + HexBytes" {
                val sut = newSut(CodePoint(0x4E2D), level = 3)
                val exercise = sut.generateEncode(level = 3, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val bitGroups = exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>()
                val hex = exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>()

                binary.length shouldBe 16
                binary.expected shouldBe "0100111000101101"
                bitGroups.expected shouldBe listOf("0100", "111000", "101101")
                hex.expected shouldBe listOf(0xE4, 0xB8, 0xAD)
            }

            "U+0800 (low boundary) -> Binary 0000100000000000, bytes E0 A0 80" {
                val sut = newSut(CodePoint(0x800), level = 3)
                val exercise = sut.generateEncode(level = 3, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0000100000000000"
                exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("0000", "100000", "000000")
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE0, 0xA0, 0x80)
            }

            "U+FFFF (high boundary, BMP max) -> bytes EF BF BF" {
                val sut = newSut(CodePoint(0xFFFF), level = 3)
                val exercise = sut.generateEncode(level = 3, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xEF, 0xBF, 0xBF)
            }
        }

        "verbose / 4-byte" - {
            // U+1F600 (😀) -> bytes F0 9F 98 80, binary 21 bits: 000011111011000000000
            // split 000 / 011111 / 011000 / 000000
            "U+1F600 (😀) -> Format + Binary(21) + BitGroups(3,6,6,6) + HexBytes" {
                val sut = newSut(CodePoint(0x1F600), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val bitGroups = exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>()
                val hex = exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>()

                binary.length shouldBe 21
                binary.expected shouldBe "000011111011000000000"
                bitGroups.expected shouldBe listOf("000", "011111", "011000", "000000")
                hex.expected shouldBe listOf(0xF0, 0x9F, 0x98, 0x80)
            }

            "U+10000 (low boundary, first supplementary) -> bytes F0 90 80 80" {
                val sut = newSut(CodePoint(0x10000), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "000010000000000000000"
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xF0, 0x90, 0x80, 0x80)
            }

            "U+10FFFF (high boundary, Unicode max) -> bytes F4 8F BF BF" {
                val sut = newSut(CodePoint(0x10FFFF), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "100001111111111111111"
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xF4, 0x8F, 0xBF, 0xBF)
            }
        }

        "standard / all byte counts produce [Format, HexBytes]" - {
            "U+0041 (1-byte)" {
                val sut = newSut(CodePoint(0x41), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.ONE_BYTE
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x41)
            }

            "U+1F600 (4-byte)" {
                val sut = newSut(CodePoint(0x1F600), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.FOUR_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xF0, 0x9F, 0x98, 0x80)
            }
        }

        "compact / all byte counts produce [HexBytes] only" - {
            "U+0041 (1-byte)" {
                val sut = newSut(CodePoint(0x41), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x41)
            }

            "U+00E9 (2-byte)" {
                val sut = newSut(CodePoint(0xE9), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xC3, 0xA9)
            }
        }

        "invalid level (encode) throws ExerciseGenerationException" {
            val sut = Utf8Generator(codec, mockk(), mockk())
            val exception = shouldThrow<ExerciseGenerationException> {
                sut.generateEncode(level = 99, Granularity.Verbose)
            }
            exception.encoding shouldBe Encoding.Utf8
            exception.level shouldBe 99
            exception.message shouldBe "Cannot generate exercise for utf-8 level 99: level must be one of: 1, 2, 3, 4"
        }

        "generateDecode" - {
            fun newDecodeSut(bytes: ByteArray, level: Int): Utf8Generator {
                val bag = mockk<ByteArrayGenerator>()
                val utf8Level = Utf8Level.fromNumber(level)!!
                every { bag.randomUtf8(utf8Level) } returns bytes
                return Utf8Generator(codec, mockk(), bag)
            }

            "verbose 1-byte [0x41] (A) -> Format + Binary(7) + CodePointEntry, NO BitGroups" {
                val sut = newDecodeSut(byteArrayOf(0x41), level = 1)
                val exercise = sut.generateDecode(level = 1, Granularity.Verbose)

                exercise.steps shouldHaveSize 3
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val cp = exercise.steps[2].shouldBeInstanceOf<Step.CodePointEntry>()

                format.expected shouldBe FormatChoice.ONE_BYTE
                binary.length shouldBe 7
                binary.expected shouldBe "1000001"
                cp.expected shouldBe 0x41
            }

            "verbose 2-byte [0xC3, 0xA9] (é) -> Format + BitGroups(5,6) + Binary(11) + CodePointEntry" {
                val sut = newDecodeSut(byteArrayOf(0xC3.toByte(), 0xA9.toByte()), level = 2)
                val exercise = sut.generateDecode(level = 2, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val bitGroups = exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>()
                val binary = exercise.steps[2].shouldBeInstanceOf<Step.Binary>()
                val cp = exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>()

                format.expected shouldBe FormatChoice.TWO_BYTES
                bitGroups.expected shouldBe listOf("00011", "101001")
                binary.length shouldBe 11
                binary.expected shouldBe "00011101001"
                cp.expected shouldBe 0xE9
            }

            "verbose 3-byte [0xE4, 0xB8, 0xAD] (中) -> BitGroups(4,6,6) + Binary(16) + CodePointEntry" {
                val sut = newDecodeSut(
                    byteArrayOf(0xE4.toByte(), 0xB8.toByte(), 0xAD.toByte()),
                    level = 3,
                )
                val exercise = sut.generateDecode(level = 3, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.THREE_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("0100", "111000", "101101")
                exercise.steps[2].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0100111000101101"
                exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x4E2D
            }

            "verbose 4-byte [0xF0, 0x9F, 0x98, 0x80] (😀) -> BitGroups(3,6,6,6) + Binary(21) + CodePointEntry" {
                val sut = newDecodeSut(
                    byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x98.toByte(), 0x80.toByte()),
                    level = 4,
                )
                val exercise = sut.generateDecode(level = 4, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.FOUR_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("000", "011111", "011000", "000000")
                exercise.steps[2].shouldBeInstanceOf<Step.Binary>().expected shouldBe "000011111011000000000"
                exercise.steps[3].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x1F600
            }

            "standard builds [Format, CodePointEntry]" {
                val sut = newDecodeSut(byteArrayOf(0xC3.toByte(), 0xA9.toByte()), level = 2)
                val exercise = sut.generateDecode(level = 2, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.TWO_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }

            "compact builds [CodePointEntry] only" {
                val sut = newDecodeSut(byteArrayOf(0xC3.toByte(), 0xA9.toByte()), level = 2)
                val exercise = sut.generateDecode(level = 2, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }
        }
    })
