package school.charset.app.domain.exercise.generator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Step

class Utf16GeneratorTest :
    FreeSpec({
        val codec = Codec()
        val generator = Utf16Generator(codec)

        "encode" - {
            "BMP code point (U+00E9 'é')" - {
                "BigEndian produces 4 steps ending in 00 E9" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.BigEndian,
                    )

                    steps.size shouldBe 4
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Format).expected shouldBe FormatChoice.ONE_CODE_UNIT
                    (steps[2] as Step.Binary).expected shouldBe "0000000011101001"
                    (steps[2] as Step.Binary).length shouldBe 16
                    (steps[3] as Step.HexBytes).expected.shouldContainExactly(0x00, 0xE9)
                }

                "LittleEndian swaps the byte order to E9 00" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.LittleEndian,
                    )

                    (steps.last() as Step.HexBytes).expected.shouldContainExactly(0xE9, 0x00)
                }
            }

            "supplementary code point (U+1F389 '🎉')" - {
                "BigEndian produces 5 steps with surrogate pair D8 3C DF 89" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.BigEndian,
                    )

                    steps.size shouldBe 5
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Format).expected shouldBe FormatChoice.TWO_CODE_UNITS
                    // 0x1F389 - 0x10000 = 0xF389 = binary 0000 1111 0011 1000 1001 padded to 20 bits
                    (steps[2] as Step.Binary).expected shouldBe "00001111001110001001"
                    (steps[2] as Step.Binary).length shouldBe 20
                    (steps[3] as Step.BitGroups).expected.shouldContainExactly("0000111100", "1110001001")
                    // High surrogate = 0xD800 + 0x3C = 0xD83C. Low = 0xDC00 + 0x389 = 0xDF89.
                    (steps[4] as Step.HexBytes).expected.shouldContainExactly(0xD8, 0x3C, 0xDF, 0x89)
                }

                "LittleEndian swaps each code unit to 3C D8 89 DF" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.LittleEndian,
                    )

                    (steps.last() as Step.HexBytes).expected.shouldContainExactly(0x3C, 0xD8, 0x89, 0xDF)
                }
            }
        }

        "decode" - {
            "BMP 2-byte input" - {
                "00 E9 BigEndian produces 4 steps ending at U+00E9" {
                    val bytes = byteArrayOf(0x00, 0xE9.toByte())
                    val steps = generator.buildDecodeStepsFor(
                        bytes = bytes,
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.BigEndian,
                    )

                    steps.size shouldBe 4
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Format).expected shouldBe FormatChoice.ONE_CODE_UNIT
                    (steps[2] as Step.Binary).expected shouldBe "0000000011101001"
                    (steps[3] as Step.CodePointEntry).expected shouldBe 0xE9
                }
            }

            "4-byte surrogate pair input" - {
                "D8 3C DF 89 BigEndian produces 5 steps ending at U+1F389" {
                    val bytes = byteArrayOf(0xD8.toByte(), 0x3C, 0xDF.toByte(), 0x89.toByte())
                    val steps = generator.buildDecodeStepsFor(
                        bytes = bytes,
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.BigEndian,
                    )

                    steps.size shouldBe 5
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Format).expected shouldBe FormatChoice.TWO_CODE_UNITS
                    (steps[2] as Step.BitGroups).expected.shouldContainExactly("0000111100", "1110001001")
                    (steps[3] as Step.Binary).expected shouldBe "00001111001110001001"
                    (steps[4] as Step.CodePointEntry).expected shouldBe 0x1F389
                }
            }
        }
    })
