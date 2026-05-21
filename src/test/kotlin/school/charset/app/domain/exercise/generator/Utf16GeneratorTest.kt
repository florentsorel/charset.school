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

class Utf16GeneratorTest :
    FreeSpec({
        val codec = Codec()
        val formatChoices = listOf(FormatChoice.TWO_BYTES, FormatChoice.FOUR_BYTES)

        fun newSut(encoding: Encoding, codePoint: CodePoint, level: Int): Utf16Generator {
            val codePointGenerator = mockk<CodePointGenerator>()
            val utf16Level = Utf16Level.fromNumber(level)!!
            every { codePointGenerator.randomUtf16(utf16Level) } returns codePoint
            return Utf16Generator(codec, codePointGenerator, encoding)
        }

        "constructor" - {
            "encoding = Utf16Be is accepted" {
                Utf16Generator(codec, mockk(), Encoding.Utf16Be).encoding shouldBe Encoding.Utf16Be
            }

            "encoding = Utf16Le is accepted" {
                Utf16Generator(codec, mockk(), Encoding.Utf16Le).encoding shouldBe Encoding.Utf16Le
            }

            "throws if encoding is not a UTF-16 variant" {
                val exception = shouldThrow<IllegalArgumentException> {
                    Utf16Generator(codec, mockk(), Encoding.Ascii)
                }
                exception.message shouldBe
                    "Utf16Generator handles only utf-16be, utf-16le, got ascii"
            }
        }

        // Structural assertions that are identical between BE and LE
        // (Format, Binary, BitGroups don't depend on byte order — only HexBytes does).
        "structural / verbose BMP — U+00E9 (é)" - {
            "Format=2 bytes, Binary(16)=0000000011101001, steps count=3" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)

                exercise.steps shouldHaveSize 3
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()

                format.choices shouldBe formatChoices
                format.expected shouldBe FormatChoice.TWO_BYTES
                binary.length shouldBe 16
                binary.expected shouldBe "0000000011101001"
            }
        }

        "structural / verbose Supplementary — U+1F600 (😀)" - {
            "Format=4 bytes, Binary(20 offset)=00001111011000000000, BitGroups=10+10, steps count=4" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0x1F600), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)

                exercise.steps shouldHaveSize 4
                val format = exercise.steps[0].shouldBeInstanceOf<Step.Format>()
                val binary = exercise.steps[1].shouldBeInstanceOf<Step.Binary>()
                val bitGroups = exercise.steps[2].shouldBeInstanceOf<Step.BitGroups>()

                format.expected shouldBe FormatChoice.FOUR_BYTES
                binary.length shouldBe 20
                binary.expected shouldBe "00001111011000000000" // 0xF600 in 20 bits
                bitGroups.expected shouldBe listOf("0000111101", "1000000000")
            }
        }

        // Byte-order-specific assertions: HexBytes content differs between BE and LE.
        "Be / verbose BMP" - {
            "U+00E9 (é) → bytes [0x00, 0xE9]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00, 0xE9)
            }

            "U+0000 (low boundary) → bytes [0x00, 0x00]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0x00), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00, 0x00)
            }

            "U+FFFF (BMP max) → bytes [0xFF, 0xFF]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0xFFFF), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xFF, 0xFF)
            }

            "U+4E2D (中) → bytes [0x4E, 0x2D]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0x4E2D), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x4E, 0x2D)
            }
        }

        "Be / verbose Supplementary" - {
            "U+1F600 (😀) → bytes [0xD8, 0x3D, 0xDE, 0x00]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0x1F600), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0xD8, 0x3D, 0xDE, 0x00)
            }

            "U+10000 (low boundary) → bytes [0xD8, 0x00, 0xDC, 0x00]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0x10000), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0xD8, 0x00, 0xDC, 0x00)
            }

            "U+10FFFF (high boundary) → bytes [0xDB, 0xFF, 0xDF, 0xFF]" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0x10FFFF), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0xDB, 0xFF, 0xDF, 0xFF)
            }
        }

        "Le / verbose BMP" - {
            "U+00E9 (é) → bytes [0xE9, 0x00] (swapped vs BE)" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0xE9), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE9, 0x00)
            }

            "U+0000 (palindromic) → bytes [0x00, 0x00]" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x00), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00, 0x00)
            }

            "U+FFFF (palindromic) → bytes [0xFF, 0xFF]" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0xFFFF), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xFF, 0xFF)
            }

            "U+4E2D (中) → bytes [0x2D, 0x4E] (swapped vs BE)" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x4E2D), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Verbose)
                exercise.steps[2].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x2D, 0x4E)
            }
        }

        "Le / verbose Supplementary" - {
            "U+1F600 (😀) → bytes [0x3D, 0xD8, 0x00, 0xDE]" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x1F600), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x3D, 0xD8, 0x00, 0xDE)
            }

            "U+10000 (low boundary) → bytes [0x00, 0xD8, 0x00, 0xDC]" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x10000), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x00, 0xD8, 0x00, 0xDC)
            }

            "U+10FFFF (high boundary) → bytes [0xFF, 0xDB, 0xFF, 0xDF]" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x10FFFF), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Verbose)
                exercise.steps[3].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0xFF, 0xDB, 0xFF, 0xDF)
            }
        }

        "standard / produces [Format, HexBytes]" - {
            "Be BMP" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.TWO_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00, 0xE9)
            }

            "Le Supplementary" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x1F600), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Standard)
                exercise.steps shouldHaveSize 2
                exercise.steps[0].shouldBeInstanceOf<Step.Format>().expected shouldBe FormatChoice.FOUR_BYTES
                exercise.steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x3D, 0xD8, 0x00, 0xDE)
            }
        }

        "compact / produces [HexBytes] only" - {
            "Be BMP" {
                val sut = newSut(Encoding.Utf16Be, CodePoint(0xE9), level = 1)
                val exercise = sut.generate(level = 1, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00, 0xE9)
            }

            "Le Supplementary" {
                val sut = newSut(Encoding.Utf16Le, CodePoint(0x1F600), level = 2)
                val exercise = sut.generate(level = 2, Granularity.Compact)
                exercise.steps shouldHaveSize 1
                exercise.steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe
                    listOf(0x3D, 0xD8, 0x00, 0xDE)
            }
        }

        "invalid level throws ExerciseGenerationException" - {
            "Be" {
                val sut = Utf16Generator(codec, mockk(), Encoding.Utf16Be)
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generate(level = 99, Granularity.Verbose)
                }
                exception.encoding shouldBe Encoding.Utf16Be
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for utf-16be level 99: level must be one of: 1, 2"
            }

            "Le" {
                val sut = Utf16Generator(codec, mockk(), Encoding.Utf16Le)
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generate(level = 99, Granularity.Verbose)
                }
                exception.encoding shouldBe Encoding.Utf16Le
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for utf-16le level 99: level must be one of: 1, 2"
            }
        }
    })
