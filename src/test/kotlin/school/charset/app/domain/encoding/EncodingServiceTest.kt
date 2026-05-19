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

        "utf-16 be" - {
            "U+0000 (NUL) -> 0x00 0x00 (low boundary)" {
                sut.encode(CodePoint(0x00), Encoding.Utf16Be).toHex() shouldBe "00 00"
            }
            "U+0041 (A) -> 0x00 0x41 (ASCII char in 16 bits)" {
                sut.encode(CodePoint(0x41), Encoding.Utf16Be).toHex() shouldBe "00 41"
            }
            "U+00E9 (é) -> 0x00 0xE9" {
                sut.encode(CodePoint(0xE9), Encoding.Utf16Be).toHex() shouldBe "00 E9"
            }
            "U+4E2D (中) -> 0x4E 0x2D" {
                sut.encode(CodePoint(0x4E2D), Encoding.Utf16Be).toHex() shouldBe "4E 2D"
            }
            "U+FFFF (non-char) -> 0xFF 0xFF (BMP max)" {
                sut.encode(CodePoint(0xFFFF), Encoding.Utf16Be).toHex() shouldBe "FF FF"
            }
            "U+10000 (𐀀) -> 0xD8 0x00 0xDC 0x00 (first supplementary, surrogate pair)" {
                sut.encode(CodePoint(0x10000), Encoding.Utf16Be).toHex() shouldBe "D8 00 DC 00"
            }
            "U+1F600 (😀) -> 0xD8 0x3D 0xDE 0x00 (emoji surrogate pair)" {
                sut.encode(CodePoint(0x1F600), Encoding.Utf16Be).toHex() shouldBe "D8 3D DE 00"
            }
            "U+10FFFF (non-char) -> 0xDB 0xFF 0xDF 0xFF (max code point)" {
                sut.encode(CodePoint(0x10FFFF), Encoding.Utf16Be).toHex() shouldBe "DB FF DF FF"
            }
            "U+D800 (first surrogate) throws" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xD800), Encoding.Utf16Be)
                }
            }
            "U+DFFF (last surrogate) throws" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xDFFF), Encoding.Utf16Be)
                }
            }
        }

        "utf-16 le" - {
            "U+0000 (NUL) -> 0x00 0x00 (low boundary, palindromic)" {
                sut.encode(CodePoint(0x00), Encoding.Utf16Le).toHex() shouldBe "00 00"
            }
            "U+0041 (A) -> 0x41 0x00 (bytes swapped vs BE)" {
                sut.encode(CodePoint(0x41), Encoding.Utf16Le).toHex() shouldBe "41 00"
            }
            "U+00E9 (é) -> 0xE9 0x00" {
                sut.encode(CodePoint(0xE9), Encoding.Utf16Le).toHex() shouldBe "E9 00"
            }
            "U+4E2D (中) -> 0x2D 0x4E" {
                sut.encode(CodePoint(0x4E2D), Encoding.Utf16Le).toHex() shouldBe "2D 4E"
            }
            "U+FFFF (non-char) -> 0xFF 0xFF (BMP max, palindromic)" {
                sut.encode(CodePoint(0xFFFF), Encoding.Utf16Le).toHex() shouldBe "FF FF"
            }
            "U+10000 (𐀀) -> 0x00 0xD8 0x00 0xDC (first supplementary, surrogate pair LE)" {
                sut.encode(CodePoint(0x10000), Encoding.Utf16Le).toHex() shouldBe "00 D8 00 DC"
            }
            "U+1F600 (😀) -> 0x3D 0xD8 0x00 0xDE" {
                sut.encode(CodePoint(0x1F600), Encoding.Utf16Le).toHex() shouldBe "3D D8 00 DE"
            }
            "U+10FFFF (non-char) -> 0xFF 0xDB 0xFF 0xDF (max code point)" {
                sut.encode(CodePoint(0x10FFFF), Encoding.Utf16Le).toHex() shouldBe "FF DB FF DF"
            }
            "U+D800 (first surrogate) throws" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xD800), Encoding.Utf16Le)
                }
            }
            "U+DFFF (last surrogate) throws" {
                shouldThrow<EncodingException> {
                    sut.encode(CodePoint(0xDFFF), Encoding.Utf16Le)
                }
            }
        }
    })
