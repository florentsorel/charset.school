package school.charset.app.domain.encoding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import school.charset.app.toHex

class EncodingServiceTest :
    FreeSpec({
        val sut = EncodingService()

        "ascii" - {
            "U+0000 (NUL) -> 0x00 (low boundary)" {
                sut.encode(CodePoint(0x00), Encoding.Ascii).toHex() shouldBe "00"
            }
            "U+0041 (A) -> 0x41" {
                sut.encode(CodePoint(0x41), Encoding.Ascii).toHex() shouldBe "41"
            }
            "U+007F (DEL) -> 0x7F (high boundary)" {
                sut.encode(CodePoint(0x7F), Encoding.Ascii).toHex() shouldBe "7F"
            }
            "U+0080 (PAD) throws, first code point outside ASCII" {
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
            "U+0000 (NUL) -> 0x00 (low boundary)" {
                sut.encode(CodePoint(0x00), Encoding.Latin1).toHex() shouldBe "00"
            }
            "U+0080 (PAD) -> 0x80 (first code point outside ASCII)" {
                sut.encode(CodePoint(0x80), Encoding.Latin1).toHex() shouldBe "80"
            }
            "U+00FF (ÿ) -> 0xFF (high boundary)" {
                sut.encode(CodePoint(0xFF), Encoding.Latin1).toHex() shouldBe "FF"
            }
            "U+00E9 (é) -> 0xE9 (canonical Latin-1 char)" {
                sut.encode(CodePoint(0xE9), Encoding.Latin1).toHex() shouldBe "E9"
            }
            "U+0100 (Ā) throws, first code point outside Latin-1" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0x100), Encoding.Latin1)
                }
            }
        }

        "utf-8" - {
            "U+0000 (NUL) -> 0x00 (low boundary)" {
                sut.encode(CodePoint(0x00), Encoding.Utf8).toHex() shouldBe "00"
            }
            "U+0041 (A) -> 0x41 (ASCII char encoded identically)" {
                sut.encode(CodePoint(0x41), Encoding.Utf8).toHex() shouldBe "41"
            }
            "U+007F (DEL) -> 0x7F (last code point in 1-byte sequence)" {
                sut.encode(CodePoint(0x7F), Encoding.Utf8).toHex() shouldBe "7F"
            }
            "U+0080 (PAD) -> 0xC2 0x80 (first code point in 2-byte sequence)" {
                sut.encode(CodePoint(0x80), Encoding.Utf8).toHex() shouldBe "C2 80"
            }
            "U+07FF (߿) -> 0xDF 0xBF (last code point in 2-byte sequence)" {
                sut.encode(CodePoint(0x7FF), Encoding.Utf8).toHex() shouldBe "DF BF"
            }
            "U+0800 (ࠀ) -> 0xE0 0xA0 0x80 (first code point in 3-byte sequence)" {
                sut.encode(CodePoint(0x800), Encoding.Utf8).toHex() shouldBe "E0 A0 80"
            }
            "U+4E2D (中) -> 0xE4 0xB8 0xAD (a common Chinese character)" {
                sut.encode(CodePoint(0x4E2D), Encoding.Utf8).toHex() shouldBe "E4 B8 AD"
            }
            "U+FFFF -> 0xEF 0xBF 0xBF (last code point in 3-byte sequence)" {
                sut.encode(CodePoint(0xFFFF), Encoding.Utf8).toHex() shouldBe "EF BF BF"
            }
            "U+10000 -> 0xF0 0x90 0x80 0x80 (first code point in 4-byte sequence)" {
                sut.encode(CodePoint(0x10000), Encoding.Utf8).toHex() shouldBe "F0 90 80 80"
            }
            "U+10FFFF -> 0xF4 0x8F 0xBF 0xBF (high boundary)" {
                sut.encode(CodePoint(0x10FFFF), Encoding.Utf8).toHex() shouldBe "F4 8F BF BF"
            }
            "U+00E9 (é) -> 0xC3 0xA9" {
                sut.encode(CodePoint(0xE9), Encoding.Utf8).toHex() shouldBe "C3 A9"
            }
            "U+1F600 (😀) -> 0xF0 0x9F 0x98 0x80" {
                sut.encode(CodePoint(0x1F600), Encoding.Utf8).toHex() shouldBe "F0 9F 98 80"
            }
            "U+D800 (first surrogate) throws" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xD800), Encoding.Utf8)
                }
            }
            "U+DFFF (last surrogate) throws" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xDFFF), Encoding.Utf8)
                }
            }
        }
    })
