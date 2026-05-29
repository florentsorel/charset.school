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
            // ASCII range: the byte IS the code point, binary-to-hex would be
            // mechanical busy-work. Verbose collapses to [Format, HexBytes] so
            // the pedagogy stays in the Format step (identity range recognition).
            "U+0041 (A) -> Format + HexBytes (no Binary, no BitGroups)" {
                val sut = newSut(CodePoint(0x41), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)

                exercise.encoding shouldBe Encoding.Utf8
                exercise.codePoint shouldBe CodePoint(0x41)
                exercise.steps shouldHaveSize 2

                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val hex = exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>()

                format.choices shouldBe formatChoices
                format.expected shouldBe FormatChoice.ONE_BYTE
                hex.expected shouldBe listOf(0x41)
            }

            "U+0000 (NUL, low boundary) -> byte 0x00" {
                val sut = newSut(CodePoint(0x00), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps shouldHaveSize 2
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00)
            }

            "U+007F (DEL, high boundary) -> byte 0x7F" {
                val sut = newSut(CodePoint(0x7F), level = 1)
                val exercise = sut.generateEncode(level = 1, Granularity.Verbose)
                exercise.steps shouldHaveSize 2
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x7F)
            }
        }

        "verbose / 2-byte" - {
            // U+00E9 (é) -> bytes C3 A9. Binary padded to 16 bits = 0000000011101001 (= 0x00E9),
            // useful bits = 11 (last 11 of the padded), split 00011 / 101001.
            "U+00E9 (é, canary) -> Format + Binary(16, padded) + UsefulBitCount(11) + BitGroups(5,6) + HexBytes" {
                val sut = newSut(CodePoint(0xE9), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)

                exercise.steps shouldHaveSize 5

                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val useful = exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>()
                val bitGroups = exercise.steps[3].shouldBeInstanceOf<Step.BitGroups>()
                val hex = exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>()

                format.expected shouldBe FormatChoice.TWO_BYTES
                binary.length shouldBe 16
                binary.expected shouldBe "0000000011101001"
                useful.expected shouldBe 11
                bitGroups.expected shouldBe listOf("00011", "101001")
                hex.expected shouldBe listOf(0xC3, 0xA9)
            }

            "U+0080 (low boundary) -> Binary 0000000010000000, BitGroups(00010, 000000), bytes C2 80" {
                val sut = newSut(CodePoint(0x80), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0000000010000000"
                exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>().expected shouldBe 11
                exercise.steps[3].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("00010", "000000")
                exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xC2, 0x80)
            }

            "U+07FF (high boundary) -> Binary 0000011111111111, BitGroups(11111, 111111), bytes DF BF" {
                val sut = newSut(CodePoint(0x7FF), level = 2)
                val exercise = sut.generateEncode(level = 2, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0000011111111111"
                exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>().expected shouldBe 11
                exercise.steps[3].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("11111", "111111")
                exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xDF, 0xBF)
            }
        }

        "verbose / 3-byte" - {
            // U+4E2D (中) -> bytes E4 B8 AD, binary 0100111000101101 (16 bits, already byte-aligned),
            // useful bits = 16, split 0100/111000/101101.
            "U+4E2D (中) -> Format + Binary(16) + UsefulBitCount(16) + BitGroups(4,6,6) + HexBytes" {
                val sut = newSut(CodePoint(0x4E2D), level = 3)
                val exercise = sut.generateEncode(level = 3, Granularity.Verbose)

                exercise.steps shouldHaveSize 5
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val useful = exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>()
                val bitGroups = exercise.steps[3].shouldBeInstanceOf<Step.BitGroups>()
                val hex = exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>()

                binary.length shouldBe 16
                binary.expected shouldBe "0100111000101101"
                useful.expected shouldBe 16
                bitGroups.expected shouldBe listOf("0100", "111000", "101101")
                hex.expected shouldBe listOf(0xE4, 0xB8, 0xAD)
            }

            "U+0800 (low boundary) -> Binary 0000100000000000, bytes E0 A0 80" {
                val sut = newSut(CodePoint(0x800), level = 3)
                val exercise = sut.generateEncode(level = 3, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0000100000000000"
                exercise.steps[3].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("0000", "100000", "000000")
                exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE0, 0xA0, 0x80)
            }

            "U+FFFF (high boundary, BMP max) -> bytes EF BF BF" {
                val sut = newSut(CodePoint(0xFFFF), level = 3)
                val exercise = sut.generateEncode(level = 3, Granularity.Verbose)
                exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xEF, 0xBF, 0xBF)
            }
        }

        "verbose / 4-byte" - {
            // U+1F600 (😀) -> bytes F0 9F 98 80. Binary padded to 24 bits = 000000011111011000000000,
            // useful bits = 21, split 000 / 011111 / 011000 / 000000.
            "U+1F600 (😀) -> Format + Binary(24, padded) + UsefulBitCount(21) + BitGroups(3,6,6,6) + HexBytes" {
                val sut = newSut(CodePoint(0x1F600), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Verbose)

                exercise.steps shouldHaveSize 5
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val useful = exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>()
                val bitGroups = exercise.steps[3].shouldBeInstanceOf<Step.BitGroups>()
                val hex = exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>()

                binary.length shouldBe 24
                binary.expected shouldBe "000000011111011000000000"
                useful.expected shouldBe 21
                bitGroups.expected shouldBe listOf("000", "011111", "011000", "000000")
                hex.expected shouldBe listOf(0xF0, 0x9F, 0x98, 0x80)
            }

            "U+10000 (low boundary, first supplementary) -> bytes F0 90 80 80" {
                val sut = newSut(CodePoint(0x10000), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "000000010000000000000000"
                exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xF0, 0x90, 0x80, 0x80)
            }

            "U+10FFFF (high boundary, Unicode max) -> bytes F4 8F BF BF" {
                val sut = newSut(CodePoint(0x10FFFF), level = 4)
                val exercise = sut.generateEncode(level = 4, Granularity.Verbose)
                exercise.steps[1].shouldBeInstanceOf<Step.Binary>().expected shouldBe "000100001111111111111111"
                exercise.steps[4].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xF4, 0x8F, 0xBF, 0xBF)
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

            "verbose 1-byte [0x41] (A) -> Format + CodePointEntry (no Binary, ASCII identity)" {
                val sut = newDecodeSut(byteArrayOf(0x41), level = 1)
                val exercise = sut.generateDecode(level = 1, Granularity.Verbose)

                exercise.steps shouldHaveSize 2
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val cp = exercise.steps[1].shouldBeInstanceOf<Step.CodePointEntry>()

                format.expected shouldBe FormatChoice.ONE_BYTE
                cp.expected shouldBe 0x41
            }

            "verbose 2-byte [0xC3, 0xA9] (é) -> Format + BitGroups(5,6) + UsefulBitCount(11) + Binary(16, padded) + CodePointEntry" {
                val sut = newDecodeSut(byteArrayOf(0xC3.toByte(), 0xA9.toByte()), level = 2)
                val exercise = sut.generateDecode(level = 2, Granularity.Verbose)

                exercise.steps shouldHaveSize 5
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val bitGroups = exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>()
                val useful = exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>()
                val binary = exercise.steps[3].shouldBeInstanceOf<Step.Binary>()
                val cp = exercise.steps[4].shouldBeInstanceOf<Step.CodePointEntry>()

                format.expected shouldBe FormatChoice.TWO_BYTES
                bitGroups.expected shouldBe listOf("00011", "101001")
                useful.expected shouldBe 11
                binary.length shouldBe 16
                binary.expected shouldBe "0000000011101001"
                cp.expected shouldBe 0xE9
            }

            "verbose 3-byte [0xE4, 0xB8, 0xAD] (中) -> BitGroups(4,6,6) + UsefulBitCount(16) + Binary(16) + CodePointEntry" {
                val sut = newDecodeSut(
                    byteArrayOf(0xE4.toByte(), 0xB8.toByte(), 0xAD.toByte()),
                    level = 3,
                )
                val exercise = sut.generateDecode(level = 3, Granularity.Verbose)

                exercise.steps shouldHaveSize 5
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.THREE_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("0100", "111000", "101101")
                exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>().expected shouldBe 16
                exercise.steps[3].shouldBeInstanceOf<Step.Binary>().expected shouldBe "0100111000101101"
                exercise.steps[4].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x4E2D
            }

            "verbose 4-byte [0xF0, 0x9F, 0x98, 0x80] (😀) -> BitGroups(3,6,6,6) + UsefulBitCount(21) + Binary(24, padded) + CodePointEntry" {
                val sut = newDecodeSut(
                    byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x98.toByte(), 0x80.toByte()),
                    level = 4,
                )
                val exercise = sut.generateDecode(level = 4, Granularity.Verbose)

                exercise.steps shouldHaveSize 5
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.FOUR_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.BitGroups>().expected shouldBe
                    listOf("000", "011111", "011000", "000000")
                exercise.steps[2].shouldBeInstanceOf<Step.UsefulBitCount>().expected shouldBe 21
                exercise.steps[3].shouldBeInstanceOf<Step.Binary>().expected shouldBe "000000011111011000000000"
                exercise.steps[4].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x1F600
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
