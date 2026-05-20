package school.charset.app.domain.encoding

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CodecTest :
    FreeSpec({
        val sut = Codec()

        "encode" - {
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
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x80), Encoding.Ascii)
                    }
                }
                "U+00E9 (é) throws, Latin-1, not representable in ASCII" {
                    shouldThrow<EncoderException> {
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
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x100), Encoding.Latin1)
                    }
                }
            }

            "windows-1252" - {
                // Identity range — same byte as Latin-1 for 0x00..0x7F and 0xA0..0xFF
                "U+0000 (NUL) -> 0x00 (low boundary)" {
                    sut.encode(CodePoint(0x00), Encoding.Windows1252).toHex() shouldBe "00"
                }
                "U+0041 (A) -> 0x41 (ASCII identity)" {
                    sut.encode(CodePoint(0x41), Encoding.Windows1252).toHex() shouldBe "41"
                }
                "U+00E9 (é) -> 0xE9 (Latin-1 identity)" {
                    sut.encode(CodePoint(0xE9), Encoding.Windows1252).toHex() shouldBe "E9"
                }
                "U+00A0 (NBSP) -> 0xA0 (above the special block)" {
                    sut.encode(CodePoint(0xA0), Encoding.Windows1252).toHex() shouldBe "A0"
                }
                "U+00FF (ÿ) -> 0xFF (high boundary)" {
                    sut.encode(CodePoint(0xFF), Encoding.Windows1252).toHex() shouldBe "FF"
                }

                // Special mappings in 0x80..0x9F (27 entries)
                "U+20AC (€) -> 0x80 (the marquee Windows-1252 character)" {
                    sut.encode(CodePoint(0x20AC), Encoding.Windows1252).toHex() shouldBe "80"
                }
                "U+0152 (Œ) -> 0x8C" {
                    sut.encode(CodePoint(0x0152), Encoding.Windows1252).toHex() shouldBe "8C"
                }
                "U+0153 (œ) -> 0x9C" {
                    sut.encode(CodePoint(0x0153), Encoding.Windows1252).toHex() shouldBe "9C"
                }
                "U+2014 (—) -> 0x97 (em dash)" {
                    sut.encode(CodePoint(0x2014), Encoding.Windows1252).toHex() shouldBe "97"
                }
                "U+2122 (™) -> 0x99 (trademark)" {
                    sut.encode(CodePoint(0x2122), Encoding.Windows1252).toHex() shouldBe "99"
                }

                "U+0080 (PAD) throws - byte 0x80 maps to € (U+20AC), not U+0080" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x0080), Encoding.Windows1252)
                    }
                }
                "U+0081 throws - unmapped (byte 0x81 is one of the 5 unassigned)" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x0081), Encoding.Windows1252)
                    }
                }
                "U+009D throws - another unassigned byte" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x009D), Encoding.Windows1252)
                    }
                }

                // Code points outside the Win-1252 representable set
                "U+0100 (Ā) throws - above Latin-1, not in special mappings" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x0100), Encoding.Windows1252)
                    }
                }
                "U+1F600 (😀) throws - supplementary plane never representable" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0x1F600), Encoding.Windows1252)
                    }
                }
                "U+D800 (first surrogate) throws" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xD800), Encoding.Windows1252)
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
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xD800), Encoding.Utf8)
                    }
                }
                "U+DFFF (last surrogate) throws" {
                    shouldThrow<EncoderException> {
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
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xD800), Encoding.Utf16Be)
                    }
                }
                "U+DFFF (last surrogate) throws" {
                    shouldThrow<EncoderException> {
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
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xD800), Encoding.Utf16Le)
                    }
                }
                "U+DFFF (last surrogate) throws" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xDFFF), Encoding.Utf16Le)
                    }
                }
            }

            "utf-32 be" - {
                "U+0000 (NUL) -> 0x00 0x00 0x00 0x00 (low boundary)" {
                    sut.encode(CodePoint(0x00), Encoding.Utf32Be).toHex() shouldBe "00 00 00 00"
                }
                "U+0041 (A) -> 0x00 0x00 0x00 0x41 (ASCII char in 32 bits)" {
                    sut.encode(CodePoint(0x41), Encoding.Utf32Be).toHex() shouldBe "00 00 00 41"
                }
                "U+00E9 (é) -> 0x00 0x00 0x00 0xE9" {
                    sut.encode(CodePoint(0xE9), Encoding.Utf32Be).toHex() shouldBe "00 00 00 E9"
                }
                "U+4E2D (中) -> 0x00 0x00 0x4E 0x2D" {
                    sut.encode(CodePoint(0x4E2D), Encoding.Utf32Be).toHex() shouldBe "00 00 4E 2D"
                }
                "U+FFFF (non-char) -> 0x00 0x00 0xFF 0xFF (BMP max)" {
                    sut.encode(CodePoint(0xFFFF), Encoding.Utf32Be).toHex() shouldBe "00 00 FF FF"
                }
                "U+10000 (𐀀) -> 0x00 0x01 0x00 0x00 (first supplementary)" {
                    sut.encode(CodePoint(0x10000), Encoding.Utf32Be).toHex() shouldBe "00 01 00 00"
                }
                "U+1F600 (😀) -> 0x00 0x01 0xF6 0x00" {
                    sut.encode(CodePoint(0x1F600), Encoding.Utf32Be).toHex() shouldBe "00 01 F6 00"
                }
                "U+10FFFF (non-char) -> 0x00 0x10 0xFF 0xFF (max code point)" {
                    sut.encode(CodePoint(0x10FFFF), Encoding.Utf32Be).toHex() shouldBe "00 10 FF FF"
                }
                "U+D800 (first surrogate) throws" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xD800), Encoding.Utf32Be)
                    }
                }
                "U+DFFF (last surrogate) throws" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xDFFF), Encoding.Utf32Be)
                    }
                }
            }

            "utf-32 le" - {
                "U+0000 (NUL) -> 0x00 0x00 0x00 0x00 (low boundary, palindromic)" {
                    sut.encode(CodePoint(0x00), Encoding.Utf32Le).toHex() shouldBe "00 00 00 00"
                }
                "U+0041 (A) -> 0x41 0x00 0x00 0x00 (bytes fully reversed vs BE)" {
                    sut.encode(CodePoint(0x41), Encoding.Utf32Le).toHex() shouldBe "41 00 00 00"
                }
                "U+00E9 (é) -> 0xE9 0x00 0x00 0x00" {
                    sut.encode(CodePoint(0xE9), Encoding.Utf32Le).toHex() shouldBe "E9 00 00 00"
                }
                "U+4E2D (中) -> 0x2D 0x4E 0x00 0x00" {
                    sut.encode(CodePoint(0x4E2D), Encoding.Utf32Le).toHex() shouldBe "2D 4E 00 00"
                }
                "U+FFFF (non-char) -> 0xFF 0xFF 0x00 0x00 (BMP max)" {
                    sut.encode(CodePoint(0xFFFF), Encoding.Utf32Le).toHex() shouldBe "FF FF 00 00"
                }
                "U+10000 (𐀀) -> 0x00 0x00 0x01 0x00 (first supplementary)" {
                    sut.encode(CodePoint(0x10000), Encoding.Utf32Le).toHex() shouldBe "00 00 01 00"
                }
                "U+1F600 (😀) -> 0x00 0xF6 0x01 0x00" {
                    sut.encode(CodePoint(0x1F600), Encoding.Utf32Le).toHex() shouldBe "00 F6 01 00"
                }
                "U+10FFFF (non-char) -> 0xFF 0xFF 0x10 0x00 (max code point)" {
                    sut.encode(CodePoint(0x10FFFF), Encoding.Utf32Le).toHex() shouldBe "FF FF 10 00"
                }
                "U+D800 (first surrogate) throws" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xD800), Encoding.Utf32Le)
                    }
                }
                "U+DFFF (last surrogate) throws" {
                    shouldThrow<EncoderException> {
                        sut.encode(CodePoint(0xDFFF), Encoding.Utf32Le)
                    }
                }
            }
        }

        "decode" - {
            "ascii" - {
                "0x00 -> U+0000 (NUL, low boundary)" {
                    sut.decode(bytes(0x00), Encoding.Ascii) shouldBe CodePoint(0x00)
                }
                "0x41 -> U+0041 (A)" {
                    sut.decode(bytes(0x41), Encoding.Ascii) shouldBe CodePoint(0x41)
                }
                "0x7F -> U+007F (DEL, high boundary)" {
                    sut.decode(bytes(0x7F), Encoding.Ascii) shouldBe CodePoint(0x7F)
                }
                "0x80 throws, high bit set, not ASCII" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x80), Encoding.Ascii)
                    }
                    exception.message shouldBe "Cannot decode [80] in ascii: high bit set, not ASCII"
                }
                "0xE9 throws, Latin-1 byte not ASCII" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xE9), Encoding.Ascii)
                    }
                }
                "0xFF throws, high byte not ASCII" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xFF), Encoding.Ascii)
                    }
                }
                "[] throws, expected 1 byte got 0" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Ascii)
                    }
                    exception.message shouldBe "Cannot decode [] in ascii: expected exactly 1 byte, got 0"
                }
                "[41 42] throws, expected 1 byte got 2" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x41, 0x42), Encoding.Ascii)
                    }
                    exception.message shouldBe "Cannot decode [41 42] in ascii: expected exactly 1 byte, got 2"
                }
            }

            "latin1" - {
                "0x41 -> U+0041 (A, ASCII)" {
                    sut.decode(bytes(0x41), Encoding.Latin1) shouldBe CodePoint(0x41)
                }
                "0x7F -> U+007F (DEL, ASCII boundary)" {
                    sut.decode(bytes(0x7F), Encoding.Latin1) shouldBe CodePoint(0x7F)
                }
                "0x00 -> U+0000 (NUL) (low boundary)" {
                    sut.decode(bytes(0x00), Encoding.Latin1) shouldBe CodePoint(0x00)
                }
                "0x80 -> U+0080 (PAD) (first code point outside ASCII)" {
                    sut.decode(bytes(0x80), Encoding.Latin1) shouldBe CodePoint(0x0080)
                }
                "0xFF -> U+00FF (ÿ) (high boundary)" {
                    sut.decode(bytes(0xFF), Encoding.Latin1) shouldBe CodePoint(0x00FF)
                }
                "0xE9 -> U+00E9 (é) (canonical Latin-1 char)" {
                    sut.decode(bytes(0xE9), Encoding.Latin1) shouldBe CodePoint(0x00E9)
                }
                "[41 42] throws, expected 1 byte got 2" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x41, 0x42), Encoding.Latin1)
                    }
                    exception.message shouldBe "Cannot decode [41 42] in latin1: expected exactly 1 byte, got 2"
                }
                "[] throws, expected 1 byte got 0" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Latin1)
                    }
                    exception.message shouldBe "Cannot decode [] in latin1: expected exactly 1 byte, got 0"
                }
            }

            "windows-1252" - {
                "0x00 -> U+0000 (NUL, low boundary)" {
                    sut.decode(bytes(0x00), Encoding.Windows1252) shouldBe CodePoint(0x0000)
                }
                "0x41 -> U+0041 (A, ASCII identity)" {
                    sut.decode(bytes(0x41), Encoding.Windows1252) shouldBe CodePoint(0x0041)
                }
                "0x7F -> U+007F (DEL, last identity-ASCII byte)" {
                    sut.decode(bytes(0x7F), Encoding.Windows1252) shouldBe CodePoint(0x007F)
                }

                "0xA0 -> U+00A0 (NBSP, first byte above the special block)" {
                    sut.decode(bytes(0xA0), Encoding.Windows1252) shouldBe CodePoint(0x00A0)
                }
                "0xE9 -> U+00E9 (é, Latin-1 identity)" {
                    sut.decode(bytes(0xE9), Encoding.Windows1252) shouldBe CodePoint(0x00E9)
                }
                "0xFF -> U+00FF (ÿ, high boundary)" {
                    sut.decode(bytes(0xFF), Encoding.Windows1252) shouldBe CodePoint(0x00FF)
                }

                // Special mappings in 0x80..0x9F
                "0x80 -> U+20AC (€, marquee character)" {
                    sut.decode(bytes(0x80), Encoding.Windows1252) shouldBe CodePoint(0x20AC)
                }
                "0x8C -> U+0152 (Œ)" {
                    sut.decode(bytes(0x8C), Encoding.Windows1252) shouldBe CodePoint(0x0152)
                }
                "0x97 -> U+2014 (—, em dash)" {
                    sut.decode(bytes(0x97), Encoding.Windows1252) shouldBe CodePoint(0x2014)
                }
                "0x99 -> U+2122 (™)" {
                    sut.decode(bytes(0x99), Encoding.Windows1252) shouldBe CodePoint(0x2122)
                }
                "0x9C -> U+0153 (œ)" {
                    sut.decode(bytes(0x9C), Encoding.Windows1252) shouldBe CodePoint(0x0153)
                }

                // Unassigned bytes - the 5 holes in 0x80..0x9F
                "0x81 throws, unassigned byte" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x81), Encoding.Windows1252)
                    }
                    exception.message shouldBe "Cannot decode [81] in windows-1252: byte 0x81 is unassigned in Windows-1252"
                }
                "0x8D throws, unassigned byte" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x8D), Encoding.Windows1252)
                    }
                }
                "0x8F throws, unassigned byte" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x8F), Encoding.Windows1252)
                    }
                }
                "0x90 throws, unassigned byte" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x90), Encoding.Windows1252)
                    }
                }
                "0x9D throws, unassigned byte" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x9D), Encoding.Windows1252)
                    }
                }

                // Size errors
                "[41 42] throws, expected 1 byte got 2" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x41, 0x42), Encoding.Windows1252)
                    }
                    exception.message shouldBe "Cannot decode [41 42] in windows-1252: expected exactly 1 byte, got 2"
                }
                "[] throws, expected 1 byte got 0" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Windows1252)
                    }
                    exception.message shouldBe "Cannot decode [] in windows-1252: expected exactly 1 byte, got 0"
                }
            }

            "utf-8" - {
                // 1-byte sequences (ASCII identity)
                "[00] -> U+0000 (NUL, low boundary)" {
                    sut.decode(bytes(0x00), Encoding.Utf8) shouldBe CodePoint(0x0000)
                }
                "[41] -> U+0041 (A)" {
                    sut.decode(bytes(0x41), Encoding.Utf8) shouldBe CodePoint(0x0041)
                }
                "[7F] -> U+007F (DEL, last 1-byte code point)" {
                    sut.decode(bytes(0x7F), Encoding.Utf8) shouldBe CodePoint(0x007F)
                }

                // 2-byte sequences
                "[C2 80] -> U+0080 (PAD, first 2-byte code point)" {
                    sut.decode(bytes(0xC2, 0x80), Encoding.Utf8) shouldBe CodePoint(0x0080)
                }
                "[C3 A9] -> U+00E9 (é, canary)" {
                    sut.decode(bytes(0xC3, 0xA9), Encoding.Utf8) shouldBe CodePoint(0x00E9)
                }
                "[DF BF] -> U+07FF (last 2-byte code point)" {
                    sut.decode(bytes(0xDF, 0xBF), Encoding.Utf8) shouldBe CodePoint(0x07FF)
                }

                // 3-byte sequences
                "[E0 A0 80] -> U+0800 (first 3-byte code point)" {
                    sut.decode(bytes(0xE0, 0xA0, 0x80), Encoding.Utf8) shouldBe CodePoint(0x0800)
                }
                "[E4 B8 AD] -> U+4E2D (中)" {
                    sut.decode(bytes(0xE4, 0xB8, 0xAD), Encoding.Utf8) shouldBe CodePoint(0x4E2D)
                }
                "[EF BF BF] -> U+FFFF (last 3-byte code point, BMP max)" {
                    sut.decode(bytes(0xEF, 0xBF, 0xBF), Encoding.Utf8) shouldBe CodePoint(0xFFFF)
                }

                // 4-byte sequences
                "[F0 90 80 80] -> U+10000 (first 4-byte code point, first supplementary)" {
                    sut.decode(bytes(0xF0, 0x90, 0x80, 0x80), Encoding.Utf8) shouldBe CodePoint(0x10000)
                }
                "[F0 9F 98 80] -> U+1F600 (😀)" {
                    sut.decode(bytes(0xF0, 0x9F, 0x98, 0x80), Encoding.Utf8) shouldBe CodePoint(0x1F600)
                }
                "[F4 8F BF BF] -> U+10FFFF (high boundary)" {
                    sut.decode(bytes(0xF4, 0x8F, 0xBF, 0xBF), Encoding.Utf8) shouldBe CodePoint(0x10FFFF)
                }

                // Empty input
                "[] throws, empty input" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [] in utf-8: empty input"
                }

                // Invalid leading bytes
                "[80] throws, continuation byte at start" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x80), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [80] in utf-8: byte 0x80 is a continuation byte, not a valid leader"
                }
                "[BF] throws, high continuation byte at start" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xBF), Encoding.Utf8)
                    }
                }
                "[F8 80 80 80 80] throws, 5-byte leader is invalid" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xF8, 0x80, 0x80, 0x80, 0x80), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [F8 80 80 80 80] in utf-8: invalid leading byte 0xF8"
                }
                "[FF] throws, 0xFF is never valid in UTF-8" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xFF), Encoding.Utf8)
                    }
                }

                // Truncated sequences
                "[C2] throws, expected 2 bytes got 1" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xC2), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [C2] in utf-8: expected 2 bytes for leading byte 0xC2, got 1"
                }
                "[E0 A0] throws, 3-byte sequence with only 1 continuation" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xE0, 0xA0), Encoding.Utf8)
                    }
                }

                // Bad continuation
                "[C2 C2] throws, second byte not a continuation (10xxxxxx)" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xC2, 0xC2), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [C2 C2] in utf-8: byte 1 (0xC2) is not a valid continuation byte"
                }

                // Overlong encodings (security-critical)
                "[C0 80] throws, overlong U+0000 in 2 bytes (must use [00])" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xC0, 0x80), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [C0 80] in utf-8: overlong encoding: U+0000 should use a shorter form"
                }
                "[E0 80 80] throws, overlong U+0000 in 3 bytes" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xE0, 0x80, 0x80), Encoding.Utf8)
                    }
                }
                "[F0 80 80 80] throws, overlong U+0000 in 4 bytes" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xF0, 0x80, 0x80, 0x80), Encoding.Utf8)
                    }
                }

                // Surrogates (security-critical)
                "[ED A0 80] throws, surrogate U+D800 not decodable" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xED, 0xA0, 0x80), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [ED A0 80] in utf-8: surrogate U+D800 not a valid code point"
                }
                "[ED BF BF] throws, surrogate U+DFFF not decodable" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xED, 0xBF, 0xBF), Encoding.Utf8)
                    }
                }

                // Above U+10FFFF
                "[F4 90 80 80] throws, U+110000 exceeds U+10FFFF" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xF4, 0x90, 0x80, 0x80), Encoding.Utf8)
                    }
                    exception.message shouldBe "Cannot decode [F4 90 80 80] in utf-8: value U+110000 exceeds U+10FFFF"
                }
                "[F5 80 80 80] throws, leader 0xF5 produces value above U+10FFFF" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xF5, 0x80, 0x80, 0x80), Encoding.Utf8)
                    }
                }
            }

            "utf-16 be" - {
                // BMP
                "[00 00] -> U+0000 (NUL, low boundary)" {
                    sut.decode(bytes(0x00, 0x00), Encoding.Utf16Be) shouldBe CodePoint(0x0000)
                }
                "[00 41] -> U+0041 (A)" {
                    sut.decode(bytes(0x00, 0x41), Encoding.Utf16Be) shouldBe CodePoint(0x0041)
                }
                "[00 E9] -> U+00E9 (é)" {
                    sut.decode(bytes(0x00, 0xE9), Encoding.Utf16Be) shouldBe CodePoint(0x00E9)
                }
                "[4E 2D] -> U+4E2D (中)" {
                    sut.decode(bytes(0x4E, 0x2D), Encoding.Utf16Be) shouldBe CodePoint(0x4E2D)
                }
                "[FF FF] -> U+FFFF (BMP max)" {
                    sut.decode(bytes(0xFF, 0xFF), Encoding.Utf16Be) shouldBe CodePoint(0xFFFF)
                }

                // Supplementary plane (surrogate pairs)
                "[D8 00 DC 00] -> U+10000 (first supplementary)" {
                    sut.decode(bytes(0xD8, 0x00, 0xDC, 0x00), Encoding.Utf16Be) shouldBe CodePoint(0x10000)
                }
                "[D8 3D DE 00] -> U+1F600 (😀)" {
                    sut.decode(bytes(0xD8, 0x3D, 0xDE, 0x00), Encoding.Utf16Be) shouldBe CodePoint(0x1F600)
                }
                "[DB FF DF FF] -> U+10FFFF (max code point)" {
                    sut.decode(bytes(0xDB, 0xFF, 0xDF, 0xFF), Encoding.Utf16Be) shouldBe CodePoint(0x10FFFF)
                }

                // Surrogate violations
                "[D8 00] throws, lone high surrogate (size 2)" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xD8, 0x00), Encoding.Utf16Be)
                    }
                    exception.message shouldBe "Cannot decode [D8 00] in utf-16be: high surrogate 0xD800 requires a following low surrogate"
                }
                "[DC 00] throws, lone low surrogate" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0xDC, 0x00), Encoding.Utf16Be)
                    }
                    exception.message shouldBe "Cannot decode [DC 00] in utf-16be: lone low surrogate 0xDC00"
                }
                "[D8 00 00 41] throws, high surrogate not followed by low surrogate" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xD8, 0x00, 0x00, 0x41), Encoding.Utf16Be)
                    }
                }
                "[D8 00 D8 00] throws, high surrogate followed by another high" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xD8, 0x00, 0xD8, 0x00), Encoding.Utf16Be)
                    }
                }

                // Size errors
                "[] throws, expected 2 or 4 bytes got 0" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Utf16Be)
                    }
                }
                "[00] throws, expected 2 or 4 bytes got 1" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00), Encoding.Utf16Be)
                    }
                }
                "[00 00 00] throws, expected 2 or 4 bytes got 3" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0x00), Encoding.Utf16Be)
                    }
                }
                "[00 41 00 42] throws, BMP code point must be exactly 2 bytes" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x41, 0x00, 0x42), Encoding.Utf16Be)
                    }
                    exception.message shouldBe "Cannot decode [00 41 00 42] in utf-16be: BMP code point 0x0041 must be exactly 2 bytes, got 4"
                }
            }

            "utf-16 le" - {
                // BMP — bytes swapped vs BE
                "[00 00] -> U+0000 (NUL, palindromic)" {
                    sut.decode(bytes(0x00, 0x00), Encoding.Utf16Le) shouldBe CodePoint(0x0000)
                }
                "[41 00] -> U+0041 (A, bytes swapped vs BE)" {
                    sut.decode(bytes(0x41, 0x00), Encoding.Utf16Le) shouldBe CodePoint(0x0041)
                }
                "[2D 4E] -> U+4E2D (中)" {
                    sut.decode(bytes(0x2D, 0x4E), Encoding.Utf16Le) shouldBe CodePoint(0x4E2D)
                }
                "[FF FF] -> U+FFFF (BMP max, palindromic)" {
                    sut.decode(bytes(0xFF, 0xFF), Encoding.Utf16Le) shouldBe CodePoint(0xFFFF)
                }

                // Supplementary plane in LE
                "[00 D8 00 DC] -> U+10000 (first supplementary, surrogate pair LE)" {
                    sut.decode(bytes(0x00, 0xD8, 0x00, 0xDC), Encoding.Utf16Le) shouldBe CodePoint(0x10000)
                }
                "[3D D8 00 DE] -> U+1F600 (😀)" {
                    sut.decode(bytes(0x3D, 0xD8, 0x00, 0xDE), Encoding.Utf16Le) shouldBe CodePoint(0x1F600)
                }
                "[FF DB FF DF] -> U+10FFFF (max code point)" {
                    sut.decode(bytes(0xFF, 0xDB, 0xFF, 0xDF), Encoding.Utf16Le) shouldBe CodePoint(0x10FFFF)
                }

                // Surrogate violations
                "[00 D8] throws, lone high surrogate" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0xD8), Encoding.Utf16Le)
                    }
                }
                "[00 DC] throws, lone low surrogate" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0xDC), Encoding.Utf16Le)
                    }
                }
                "[00 D8 41 00] throws, high surrogate not followed by low (LE)" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0xD8, 0x41, 0x00), Encoding.Utf16Le)
                    }
                }

                // Size errors
                "[] throws, expected 2 or 4 bytes got 0" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Utf16Le)
                    }
                }
                "[00 00 00] throws, expected 2 or 4 bytes got 3" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0x00), Encoding.Utf16Le)
                    }
                }
            }

            "utf-32 be" - {
                // Success — every 4-byte BE value mapping a code point
                "[00 00 00 00] -> U+0000 (NUL, low boundary)" {
                    sut.decode(bytes(0x00, 0x00, 0x00, 0x00), Encoding.Utf32Be) shouldBe CodePoint(0x0000)
                }
                "[00 00 00 41] -> U+0041 (A)" {
                    sut.decode(bytes(0x00, 0x00, 0x00, 0x41), Encoding.Utf32Be) shouldBe CodePoint(0x0041)
                }
                "[00 00 00 E9] -> U+00E9 (é)" {
                    sut.decode(bytes(0x00, 0x00, 0x00, 0xE9), Encoding.Utf32Be) shouldBe CodePoint(0x00E9)
                }
                "[00 00 4E 2D] -> U+4E2D (中)" {
                    sut.decode(bytes(0x00, 0x00, 0x4E, 0x2D), Encoding.Utf32Be) shouldBe CodePoint(0x4E2D)
                }
                "[00 00 FF FF] -> U+FFFF (BMP max)" {
                    sut.decode(bytes(0x00, 0x00, 0xFF, 0xFF), Encoding.Utf32Be) shouldBe CodePoint(0xFFFF)
                }
                "[00 01 00 00] -> U+10000 (first supplementary)" {
                    sut.decode(bytes(0x00, 0x01, 0x00, 0x00), Encoding.Utf32Be) shouldBe CodePoint(0x10000)
                }
                "[00 01 F6 00] -> U+1F600 (😀)" {
                    sut.decode(bytes(0x00, 0x01, 0xF6, 0x00), Encoding.Utf32Be) shouldBe CodePoint(0x1F600)
                }
                "[00 10 FF FF] -> U+10FFFF (max code point)" {
                    sut.decode(bytes(0x00, 0x10, 0xFF, 0xFF), Encoding.Utf32Be) shouldBe CodePoint(0x10FFFF)
                }

                // Surrogates
                "[00 00 D8 00] throws, surrogate U+D800" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0xD8, 0x00), Encoding.Utf32Be)
                    }
                    exception.message shouldBe "Cannot decode [00 00 D8 00] in utf-32be: surrogate U+D800 not a valid code point"
                }
                "[00 00 DF FF] throws, surrogate U+DFFF" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0xDF, 0xFF), Encoding.Utf32Be)
                    }
                }

                // Above U+10FFFF
                "[00 11 00 00] throws, U+110000 exceeds U+10FFFF" {
                    val exception = shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x11, 0x00, 0x00), Encoding.Utf32Be)
                    }
                    exception.message shouldBe "Cannot decode [00 11 00 00] in utf-32be: value 0x110000 exceeds U+10FFFF"
                }
                "[FF FF FF FF] throws, far above range (catches sign-extension bug)" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xFF, 0xFF, 0xFF, 0xFF), Encoding.Utf32Be)
                    }
                }

                // Size errors
                "[] throws, expected 4 bytes got 0" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(), Encoding.Utf32Be)
                    }
                }
                "[00] throws, expected 4 bytes got 1" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00), Encoding.Utf32Be)
                    }
                }
                "[00 00 00] throws, expected 4 bytes got 3" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0x00), Encoding.Utf32Be)
                    }
                }
                "[00 00 00 00 00] throws, expected 4 bytes got 5" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0x00, 0x00, 0x00), Encoding.Utf32Be)
                    }
                }
            }

            "utf-32 le" - {
                // Success — bytes fully reversed vs BE
                "[00 00 00 00] -> U+0000 (NUL, palindromic)" {
                    sut.decode(bytes(0x00, 0x00, 0x00, 0x00), Encoding.Utf32Le) shouldBe CodePoint(0x0000)
                }
                "[41 00 00 00] -> U+0041 (A, bytes fully reversed)" {
                    sut.decode(bytes(0x41, 0x00, 0x00, 0x00), Encoding.Utf32Le) shouldBe CodePoint(0x0041)
                }
                "[2D 4E 00 00] -> U+4E2D (中)" {
                    sut.decode(bytes(0x2D, 0x4E, 0x00, 0x00), Encoding.Utf32Le) shouldBe CodePoint(0x4E2D)
                }
                "[00 00 01 00] -> U+10000 (first supplementary)" {
                    sut.decode(bytes(0x00, 0x00, 0x01, 0x00), Encoding.Utf32Le) shouldBe CodePoint(0x10000)
                }
                "[00 F6 01 00] -> U+1F600 (😀)" {
                    sut.decode(bytes(0x00, 0xF6, 0x01, 0x00), Encoding.Utf32Le) shouldBe CodePoint(0x1F600)
                }
                "[FF FF 10 00] -> U+10FFFF (max code point)" {
                    sut.decode(bytes(0xFF, 0xFF, 0x10, 0x00), Encoding.Utf32Le) shouldBe CodePoint(0x10FFFF)
                }

                // Surrogates in LE
                "[00 D8 00 00] throws, surrogate U+D800" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0xD8, 0x00, 0x00), Encoding.Utf32Le)
                    }
                }
                "[FF DF 00 00] throws, surrogate U+DFFF" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xFF, 0xDF, 0x00, 0x00), Encoding.Utf32Le)
                    }
                }

                // Above range
                "[00 00 11 00] throws, U+110000 exceeds U+10FFFF" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0x00, 0x00, 0x11, 0x00), Encoding.Utf32Le)
                    }
                }
                "[FF FF FF FF] throws, far above range" {
                    shouldThrow<DecoderException> {
                        sut.decode(bytes(0xFF, 0xFF, 0xFF, 0xFF), Encoding.Utf32Le)
                    }
                }
            }
        }
    })
