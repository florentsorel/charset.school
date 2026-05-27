package school.charset.app.domain.exercise.generator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Utf32GeneratorTest :
    FreeSpec({
        val codec = Codec()
        val generator = Utf32Generator(codec)

        "encode" - {
            "small code point (U+00E9 'é')" - {
                "BigEndian produces 3 verbose steps ending in 00 00 00 E9" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.BigEndian,
                        granularity = Granularity.Verbose,
                    )

                    steps.size shouldBe 3
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Binary).expected shouldBe "00000000000000000000000011101001"
                    (steps[1] as Step.Binary).length shouldBe 32
                    (steps[2] as Step.HexBytes).expected.shouldContainExactly(0x00, 0x00, 0x00, 0xE9)
                }

                "LittleEndian reverses the byte order to E9 00 00 00" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.LittleEndian,
                        granularity = Granularity.Verbose,
                    )

                    (steps.last() as Step.HexBytes).expected.shouldContainExactly(0xE9, 0x00, 0x00, 0x00)
                }
            }

            "supplementary code point (U+1F389 'tada')" - {
                "BigEndian produces 3 verbose steps ending in 00 01 F3 89" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.BigEndian,
                        granularity = Granularity.Verbose,
                    )

                    steps.size shouldBe 3
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Binary).expected shouldBe "00000000000000011111001110001001"
                    (steps[1] as Step.Binary).length shouldBe 32
                    (steps[2] as Step.HexBytes).expected.shouldContainExactly(0x00, 0x01, 0xF3, 0x89)
                }

                "LittleEndian reverses the byte order to 89 F3 01 00" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.LittleEndian,
                        granularity = Granularity.Verbose,
                    )

                    (steps.last() as Step.HexBytes).expected.shouldContainExactly(0x89, 0xF3, 0x01, 0x00)
                }
            }

            "max code point (U+10FFFF)" - {
                "BigEndian produces 4 bytes 00 10 FF FF" {
                    val steps = generator.buildEncodeStepsFor(
                        codePoint = CodePoint(0x10FFFF),
                        endian = Encoding.Endian.BigEndian,
                        granularity = Granularity.Verbose,
                    )

                    (steps[1] as Step.Binary).expected shouldBe "00000000000100001111111111111111"
                    (steps.last() as Step.HexBytes).expected.shouldContainExactly(0x00, 0x10, 0xFF, 0xFF)
                }
            }

            "Standard granularity drops the binary step" {
                val steps = generator.buildEncodeStepsFor(
                    codePoint = CodePoint(0xE9),
                    endian = Encoding.Endian.BigEndian,
                    granularity = Granularity.Standard,
                )

                steps.size shouldBe 2
                (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                (steps[1] as Step.HexBytes).expected.shouldContainExactly(0x00, 0x00, 0x00, 0xE9)
            }

            "Compact granularity keeps only the hex bytes" {
                val steps = generator.buildEncodeStepsFor(
                    codePoint = CodePoint(0xE9),
                    endian = Encoding.Endian.BigEndian,
                    granularity = Granularity.Compact,
                )

                steps.size shouldBe 1
                (steps[0] as Step.HexBytes).expected.shouldContainExactly(0x00, 0x00, 0x00, 0xE9)
            }
        }

        "decode" - {
            "small code point input" - {
                "00 00 00 E9 BigEndian produces 3 verbose steps ending at U+00E9" {
                    val bytes = byteArrayOf(0x00, 0x00, 0x00, 0xE9.toByte())
                    val steps = generator.buildDecodeStepsFor(
                        bytes = bytes,
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.BigEndian,
                        granularity = Granularity.Verbose,
                    )

                    steps.size shouldBe 3
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Binary).expected shouldBe "00000000000000000000000011101001"
                    (steps[1] as Step.Binary).length shouldBe 32
                    (steps[2] as Step.CodePointEntry).expected shouldBe 0xE9
                }

                "E9 00 00 00 LittleEndian also decodes to U+00E9" {
                    val bytes = byteArrayOf(0xE9.toByte(), 0x00, 0x00, 0x00)
                    val steps = generator.buildDecodeStepsFor(
                        bytes = bytes,
                        codePoint = CodePoint(0xE9),
                        endian = Encoding.Endian.LittleEndian,
                        granularity = Granularity.Verbose,
                    )

                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.LittleEndian
                    (steps.last() as Step.CodePointEntry).expected shouldBe 0xE9
                }
            }

            "supplementary code point input" - {
                "00 01 F3 89 BigEndian produces 3 verbose steps ending at U+1F389" {
                    val bytes = byteArrayOf(0x00, 0x01, 0xF3.toByte(), 0x89.toByte())
                    val steps = generator.buildDecodeStepsFor(
                        bytes = bytes,
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.BigEndian,
                        granularity = Granularity.Verbose,
                    )

                    steps.size shouldBe 3
                    (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                    (steps[1] as Step.Binary).expected shouldBe "00000000000000011111001110001001"
                    (steps[2] as Step.CodePointEntry).expected shouldBe 0x1F389
                }

                "89 F3 01 00 LittleEndian also decodes to U+1F389" {
                    val bytes = byteArrayOf(0x89.toByte(), 0xF3.toByte(), 0x01, 0x00)
                    val steps = generator.buildDecodeStepsFor(
                        bytes = bytes,
                        codePoint = CodePoint(0x1F389),
                        endian = Encoding.Endian.LittleEndian,
                        granularity = Granularity.Verbose,
                    )

                    (steps.last() as Step.CodePointEntry).expected shouldBe 0x1F389
                }
            }

            "Standard granularity drops the binary step" {
                val bytes = byteArrayOf(0x00, 0x00, 0x00, 0xE9.toByte())
                val steps = generator.buildDecodeStepsFor(
                    bytes = bytes,
                    codePoint = CodePoint(0xE9),
                    endian = Encoding.Endian.BigEndian,
                    granularity = Granularity.Standard,
                )

                steps.size shouldBe 2
                (steps[0] as Step.Endianness).expected shouldBe Encoding.Endian.BigEndian
                (steps[1] as Step.CodePointEntry).expected shouldBe 0xE9
            }

            "Compact granularity keeps only the code point entry" {
                val bytes = byteArrayOf(0x00, 0x00, 0x00, 0xE9.toByte())
                val steps = generator.buildDecodeStepsFor(
                    bytes = bytes,
                    codePoint = CodePoint(0xE9),
                    endian = Encoding.Endian.BigEndian,
                    granularity = Granularity.Compact,
                )

                steps.size shouldBe 1
                (steps[0] as Step.CodePointEntry).expected shouldBe 0xE9
            }
        }
    })
