package school.charset.app.domain.encoding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import school.charset.app.toHex

class EncodingServiceTest :
    FreeSpec({
        val sut = EncodingService()

        "ascii" - {
            "U+0000 -> 0x00 (low boundary)" {
                sut.encode(CodePoint(0x00), Encoding.Ascii).toHex() shouldBe "00"
            }
            "U+0041 (A) -> 0x41" {
                sut.encode(CodePoint(0x41), Encoding.Ascii).toHex() shouldBe "41"
            }
            "U+007F -> 0x7F (high boundary)" {
                sut.encode(CodePoint(0x7F), Encoding.Ascii).toHex() shouldBe "7F"
            }
            "U+0080 throws, first code point outside ASCII" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0x80), Encoding.Ascii)
                }
            }
            "U+00E9 (é) throws, Latin-1, not representable in ASCII" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xE9), Encoding.Ascii)
                }
            }
        }
        "latin1" - {
            "U+0000 -> 0x00 (low boundary)" {
                sut.encode(CodePoint(0x00), Encoding.Ascii).toHex() shouldBe "00"
            }
            "U+0080 -> 0x80 (first code point outside ASCII)" {
                sut.encode(CodePoint(0x80), Encoding.Latin1).toHex() shouldBe "80"
            }
            "U+00FF -> 0xFF (high boundary)" {
                sut.encode(CodePoint(0xFF), Encoding.Latin1).toHex() shouldBe "FF"
            }
            "U+0100 throws, first code point outside Latin-1" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0x100), Encoding.Latin1)
                }
            }
        }
    })
