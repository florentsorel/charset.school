package school.charset.app.domain.exercise.generator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.EncoderException
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step

class Latin1GeneratorTest :
    FreeSpec({
        val codec = Codec()
        val codePointGenerator = CodePointGenerator(kotlin.random.Random.Default)
        val byteArrayGenerator = ByteArrayGenerator(codec, codePointGenerator)
        val sut = Latin1Generator(codec, codePointGenerator, byteArrayGenerator)

        "buildEncodeStepsFor" - {
            "ASCII U+0041 (A) verbose: Binary(8)=01000001, HexBytes=[0x41]" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x41), Granularity.Verbose)
                steps shouldHaveSize 2
                steps[0].shouldBeInstanceOf<Step.Binary>().also {
                    it.expected shouldBe "01000001"
                    it.length shouldBe 8
                }
                steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x41)
            }

            "Latin-1 extended U+00E9 (é) verbose: Binary(8)=11101001, HexBytes=[0xE9]" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0xE9), Granularity.Verbose)
                steps shouldHaveSize 2
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "11101001"
                steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE9)
            }

            "low boundary U+0000 (NUL) verbose: Binary(8)=00000000" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0x00), Granularity.Verbose)
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "00000000"
                steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0x00)
            }

            "high boundary U+00FF (ÿ) verbose: Binary(8)=11111111" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0xFF), Granularity.Verbose)
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "11111111"
                steps[1].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xFF)
            }

            "standard granularity drops Binary, keeps HexBytes" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0xE9), Granularity.Standard)
                steps shouldHaveSize 1
                steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE9)
            }

            "compact granularity drops Binary, keeps HexBytes" {
                val steps = sut.buildEncodeStepsFor(CodePoint(0xE9), Granularity.Compact)
                steps shouldHaveSize 1
                steps[0].shouldBeInstanceOf<Step.HexBytes>().expected shouldBe listOf(0xE9)
            }

            "throws EncoderException when code point exceeds U+00FF" {
                // The encode step builder delegates to Codec which enforces
                // the Latin-1 range; the controller layer catches this and
                // maps to a 422 `encoding.not-encodable` response.
                shouldThrow<EncoderException> {
                    sut.buildEncodeStepsFor(CodePoint(0x100), Granularity.Verbose)
                }
            }
        }

        "buildDecodeStepsFor" - {
            "ASCII 0x41 verbose: Binary(8)=01000001, CodePointEntry=0x41" {
                val steps = sut.buildDecodeStepsFor(byteArrayOf(0x41), Granularity.Verbose)
                steps shouldHaveSize 2
                steps[0].shouldBeInstanceOf<Step.Binary>().also {
                    it.expected shouldBe "01000001"
                    it.length shouldBe 8
                }
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x41
            }

            "Latin-1 extended 0xE9 verbose: Binary(8)=11101001, CodePointEntry=0xE9" {
                val steps = sut.buildDecodeStepsFor(byteArrayOf(0xE9.toByte()), Granularity.Verbose)
                steps shouldHaveSize 2
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "11101001"
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }

            "low boundary 0x00 (NUL) verbose: Binary(8)=00000000" {
                val steps = sut.buildDecodeStepsFor(byteArrayOf(0x00), Granularity.Verbose)
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "00000000"
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0x00
            }

            "high boundary 0xFF (ÿ) verbose: Binary(8)=11111111" {
                val steps = sut.buildDecodeStepsFor(byteArrayOf(0xFF.toByte()), Granularity.Verbose)
                steps[0].shouldBeInstanceOf<Step.Binary>().expected shouldBe "11111111"
                steps[1].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xFF
            }

            "standard granularity drops Binary, keeps CodePointEntry" {
                val steps = sut.buildDecodeStepsFor(byteArrayOf(0xE9.toByte()), Granularity.Standard)
                steps shouldHaveSize 1
                steps[0].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }

            "compact granularity drops Binary, keeps CodePointEntry" {
                val steps = sut.buildDecodeStepsFor(byteArrayOf(0xE9.toByte()), Granularity.Compact)
                steps shouldHaveSize 1
                steps[0].shouldBeInstanceOf<Step.CodePointEntry>().expected shouldBe 0xE9
            }

            "rejects multi-byte input as a domain invariant violation" {
                // Latin-1 is fixed-width. Callers MUST validate size before
                // invoking the builder; this guard exists purely for
                // debuggability if something else ever calls in.
                shouldThrow<IllegalArgumentException> {
                    sut.buildDecodeStepsFor(byteArrayOf(0xC3.toByte(), 0xA9.toByte()), Granularity.Verbose)
                }
            }
        }
    })
