package school.charset.app.domain.exercise.generator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding

class ByteArrayGeneratorTest :
    FreeSpec({
        val codec = Codec()

        // ByteArrayGenerator delegates to CodePointGenerator + Codec.encode.
        // Tests pin the code point returned by the mocked CodePointGenerator;
        // the byte output is whatever Codec.encode produces (real Codec used).

        "randomAscii" - {
            "U+0041 (A) -> byte [0x41]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomAscii(AsciiLevel.Printable) } returns CodePoint(0x41)
                ByteArrayGenerator(codec, cpg).randomAscii(AsciiLevel.Printable) shouldBe byteArrayOf(0x41)
            }

            "U+0000 (NUL) -> byte [0x00]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomAscii(AsciiLevel.Full) } returns CodePoint(0x00)
                ByteArrayGenerator(codec, cpg).randomAscii(AsciiLevel.Full) shouldBe byteArrayOf(0x00)
            }
        }

        "randomLatin1" - {
            "U+00E9 (é) -> byte [0xE9]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomLatin1(Latin1Level.Supplement) } returns CodePoint(0xE9)
                ByteArrayGenerator(codec, cpg).randomLatin1(Latin1Level.Supplement) shouldBe byteArrayOf(0xE9.toByte())
            }

            "U+0000 (NUL) -> byte [0x00]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomLatin1(Latin1Level.Full) } returns CodePoint(0x00)
                ByteArrayGenerator(codec, cpg).randomLatin1(Latin1Level.Full) shouldBe byteArrayOf(0x00)
            }
        }

        "randomWindows1252" - {
            "U+20AC (€, special block) -> byte [0x80]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomWindows1252(Windows1252Level.SpecialBlock) } returns CodePoint(0x20AC)
                ByteArrayGenerator(codec, cpg).randomWindows1252(Windows1252Level.SpecialBlock) shouldBe
                    byteArrayOf(0x80.toByte())
            }

            "U+0041 (A, ASCII identity) -> byte [0x41]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomWindows1252(Windows1252Level.AllEncodable) } returns CodePoint(0x41)
                ByteArrayGenerator(codec, cpg).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    byteArrayOf(0x41)
            }
        }

        "randomUtf8" - {
            "U+0041 (A, 1-byte) -> bytes [0x41]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf8(Utf8Level.OneByte) } returns CodePoint(0x41)
                ByteArrayGenerator(codec, cpg).randomUtf8(Utf8Level.OneByte) shouldBe byteArrayOf(0x41)
            }

            "U+00E9 (é, 2-byte) -> bytes [0xC3, 0xA9]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf8(Utf8Level.TwoByte) } returns CodePoint(0xE9)
                ByteArrayGenerator(codec, cpg).randomUtf8(Utf8Level.TwoByte) shouldBe
                    byteArrayOf(0xC3.toByte(), 0xA9.toByte())
            }

            "U+1F600 (😀, 4-byte) -> bytes [0xF0, 0x9F, 0x98, 0x80]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf8(Utf8Level.FourByte) } returns CodePoint(0x1F600)
                ByteArrayGenerator(codec, cpg).randomUtf8(Utf8Level.FourByte) shouldBe
                    byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x98.toByte(), 0x80.toByte())
            }
        }

        "randomUtf16" - {
            "BMP U+00E9 BE -> bytes [0x00, 0xE9]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf16(Utf16Level.Bmp) } returns CodePoint(0xE9)
                ByteArrayGenerator(codec, cpg).randomUtf16(Utf16Level.Bmp, Encoding.Utf16Be) shouldBe
                    byteArrayOf(0x00, 0xE9.toByte())
            }

            "BMP U+00E9 LE -> bytes [0xE9, 0x00] (bytes swapped)" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf16(Utf16Level.Bmp) } returns CodePoint(0xE9)
                ByteArrayGenerator(codec, cpg).randomUtf16(Utf16Level.Bmp, Encoding.Utf16Le) shouldBe
                    byteArrayOf(0xE9.toByte(), 0x00)
            }

            "Supplementary U+1F600 BE -> bytes [0xD8, 0x3D, 0xDE, 0x00]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf16(Utf16Level.Supplementary) } returns CodePoint(0x1F600)
                ByteArrayGenerator(codec, cpg).randomUtf16(Utf16Level.Supplementary, Encoding.Utf16Be) shouldBe
                    byteArrayOf(0xD8.toByte(), 0x3D, 0xDE.toByte(), 0x00)
            }

            "rejects non-UTF-16 encoding" {
                val cpg = mockk<CodePointGenerator>()
                val exception = shouldThrow<IllegalArgumentException> {
                    ByteArrayGenerator(codec, cpg).randomUtf16(Utf16Level.Bmp, Encoding.Ascii)
                }
                exception.message shouldBe "randomUtf16 requires Utf16Be or Utf16Le, got ascii"
            }
        }

        "randomUtf32" - {
            "U+0041 (A) BE -> bytes [0x00, 0x00, 0x00, 0x41]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf32(Utf32Level.Bmp) } returns CodePoint(0x41)
                ByteArrayGenerator(codec, cpg).randomUtf32(Utf32Level.Bmp, Encoding.Utf32Be) shouldBe
                    byteArrayOf(0x00, 0x00, 0x00, 0x41)
            }

            "U+0041 (A) LE -> bytes [0x41, 0x00, 0x00, 0x00] (fully reversed)" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf32(Utf32Level.Bmp) } returns CodePoint(0x41)
                ByteArrayGenerator(codec, cpg).randomUtf32(Utf32Level.Bmp, Encoding.Utf32Le) shouldBe
                    byteArrayOf(0x41, 0x00, 0x00, 0x00)
            }

            "U+1F600 BE -> bytes [0x00, 0x01, 0xF6, 0x00]" {
                val cpg = mockk<CodePointGenerator>()
                every { cpg.randomUtf32(Utf32Level.Supplementary) } returns CodePoint(0x1F600)
                ByteArrayGenerator(codec, cpg).randomUtf32(Utf32Level.Supplementary, Encoding.Utf32Be) shouldBe
                    byteArrayOf(0x00, 0x01, 0xF6.toByte(), 0x00)
            }

            "rejects non-UTF-32 encoding" {
                val cpg = mockk<CodePointGenerator>()
                val exception = shouldThrow<IllegalArgumentException> {
                    ByteArrayGenerator(codec, cpg).randomUtf32(Utf32Level.Bmp, Encoding.Ascii)
                }
                exception.message shouldBe "randomUtf32 requires Utf32Be or Utf32Le, got ascii"
            }
        }
    })
