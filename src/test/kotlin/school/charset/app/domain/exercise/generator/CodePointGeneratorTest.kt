package school.charset.app.domain.exercise.generator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import kotlin.random.Random

class CodePointGeneratorTest :
    FreeSpec({
        // Random is mocked with MockK. Each test pins both the bounds passed to
        // `Random.nextInt(from, until)` (via `every` with explicit arguments) and
        // the value returned. If the production code passes wrong bounds, MockK
        // throws "no mocking found" and the test fails fast.

        "randomAscii" - {
            "Printable - picks from [0x20, 0x7F) = printable ASCII" {
                val random = mockk<Random>()
                every { random.nextInt(0x20, 0x7F) } returns 0x41
                CodePointGenerator(random).randomAscii(AsciiLevel.Printable) shouldBe CodePoint(0x41)
            }

            "Printable - low boundary 0x20" {
                val random = mockk<Random>()
                every { random.nextInt(0x20, 0x7F) } returns 0x20
                CodePointGenerator(random).randomAscii(AsciiLevel.Printable) shouldBe CodePoint(0x20)
            }

            "Printable - high boundary 0x7E" {
                val random = mockk<Random>()
                every { random.nextInt(0x20, 0x7F) } returns 0x7E
                CodePointGenerator(random).randomAscii(AsciiLevel.Printable) shouldBe CodePoint(0x7E)
            }

            "Full - picks from [0x00, 0x80) = full ASCII including controls" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x41
                CodePointGenerator(random).randomAscii(AsciiLevel.Full) shouldBe CodePoint(0x41)
            }

            "Full - low boundary 0x00 (NUL)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x00
                CodePointGenerator(random).randomAscii(AsciiLevel.Full) shouldBe CodePoint(0x00)
            }

            "Full - high boundary 0x7F (DEL)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x7F
                CodePointGenerator(random).randomAscii(AsciiLevel.Full) shouldBe CodePoint(0x7F)
            }
        }

        "randomLatin1" - {
            "Supplement - picks from [0xA0, 0x100) = Latin-1 supplement only" {
                val random = mockk<Random>()
                every { random.nextInt(0xA0, 0x100) } returns 0xE9
                CodePointGenerator(random).randomLatin1(Latin1Level.Supplement) shouldBe CodePoint(0xE9)
            }

            "Supplement - low boundary 0xA0 (NBSP)" {
                val random = mockk<Random>()
                every { random.nextInt(0xA0, 0x100) } returns 0xA0
                CodePointGenerator(random).randomLatin1(Latin1Level.Supplement) shouldBe CodePoint(0xA0)
            }

            "Supplement - high boundary 0xFF (ÿ)" {
                val random = mockk<Random>()
                every { random.nextInt(0xA0, 0x100) } returns 0xFF
                CodePointGenerator(random).randomLatin1(Latin1Level.Supplement) shouldBe CodePoint(0xFF)
            }

            "Full - picks from [0x00, 0x100) = full Latin-1" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x100) } returns 0xE9
                CodePointGenerator(random).randomLatin1(Latin1Level.Full) shouldBe CodePoint(0xE9)
            }

            "Full - low boundary 0x00 (NUL)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x100) } returns 0x00
                CodePointGenerator(random).randomLatin1(Latin1Level.Full) shouldBe CodePoint(0x00)
            }

            "Full - high boundary 0xFF (ÿ)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x100) } returns 0xFF
                CodePointGenerator(random).randomLatin1(Latin1Level.Full) shouldBe CodePoint(0xFF)
            }
        }

        "randomWindows1252" - {
            // SpecialBlock picks from the 27 special code points only.
            // Indices are stable: 0 = byte 0x80 (€), ..., 26 = byte 0x9F (Ÿ).

            "SpecialBlock - index 0 picks Euro (U+20AC, byte 0x80)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 27) } returns 0
                CodePointGenerator(random).randomWindows1252(Windows1252Level.SpecialBlock) shouldBe
                    CodePoint(0x20AC)
            }

            "SpecialBlock - index 11 picks Œ (U+0152, byte 0x8C)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 27) } returns 11
                CodePointGenerator(random).randomWindows1252(Windows1252Level.SpecialBlock) shouldBe
                    CodePoint(0x0152)
            }

            "SpecialBlock - index 26 picks Ÿ (U+0178, byte 0x9F, last special)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 27) } returns 26
                CodePointGenerator(random).randomWindows1252(Windows1252Level.SpecialBlock) shouldBe
                    CodePoint(0x0178)
            }

            // AllEncodable picks from all 251 encodable code points. Layout:
            // - indices 0..127     : ASCII range (U+0000..U+007F)
            // - indices 128..154   : special block (27 entries, in byte order)
            // - indices 155..250   : Latin-1 supplement (U+00A0..U+00FF)

            "AllEncodable - index 0 picks U+0000 (NUL, start of ASCII)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 0
                CodePointGenerator(random).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    CodePoint(0x00)
            }

            "AllEncodable - index 127 picks U+007F (DEL, end of ASCII)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 127
                CodePointGenerator(random).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    CodePoint(0x7F)
            }

            "AllEncodable - index 128 picks U+20AC (Euro, first special)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 128
                CodePointGenerator(random).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    CodePoint(0x20AC)
            }

            "AllEncodable - index 154 picks U+0178 (Ÿ, last special)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 154
                CodePointGenerator(random).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    CodePoint(0x0178)
            }

            "AllEncodable - index 155 picks U+00A0 (NBSP, start of Latin-1 supplement)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 155
                CodePointGenerator(random).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    CodePoint(0xA0)
            }

            "AllEncodable - index 250 picks U+00FF (ÿ, end)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 250
                CodePointGenerator(random).randomWindows1252(Windows1252Level.AllEncodable) shouldBe
                    CodePoint(0xFF)
            }
        }

        "randomUtf8" - {
            "OneByte - picks from [0x00, 0x80) = 1-byte UTF-8 (ASCII subset)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x41
                CodePointGenerator(random).randomUtf8(Utf8Level.OneByte) shouldBe CodePoint(0x41)
            }

            "OneByte - low boundary 0x00" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x00
                CodePointGenerator(random).randomUtf8(Utf8Level.OneByte) shouldBe CodePoint(0x00)
            }

            "OneByte - high boundary 0x7F" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x7F
                CodePointGenerator(random).randomUtf8(Utf8Level.OneByte) shouldBe CodePoint(0x7F)
            }

            "TwoByte - picks from [0x80, 0x800) = 2-byte UTF-8" {
                val random = mockk<Random>()
                every { random.nextInt(0x80, 0x800) } returns 0xE9
                CodePointGenerator(random).randomUtf8(Utf8Level.TwoByte) shouldBe CodePoint(0xE9)
            }

            "TwoByte - low boundary 0x80" {
                val random = mockk<Random>()
                every { random.nextInt(0x80, 0x800) } returns 0x80
                CodePointGenerator(random).randomUtf8(Utf8Level.TwoByte) shouldBe CodePoint(0x80)
            }

            "TwoByte - high boundary 0x7FF" {
                val random = mockk<Random>()
                every { random.nextInt(0x80, 0x800) } returns 0x7FF
                CodePointGenerator(random).randomUtf8(Utf8Level.TwoByte) shouldBe CodePoint(0x7FF)
            }

            // ThreeByte: 3-byte range with surrogate gap. The implementation indexes
            // into a virtual range of 61440 non-surrogate code points:
            // - indices 0..53247   -> U+0800..U+D7FF (before surrogates)
            // - indices 53248..61439 -> U+E000..U+FFFF (after surrogates)

            "ThreeByte - index 0 picks U+0800 (start of 3-byte range)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 61440) } returns 0
                CodePointGenerator(random).randomUtf8(Utf8Level.ThreeByte) shouldBe CodePoint(0x0800)
            }

            "ThreeByte - index 53247 picks U+D7FF (last before surrogates)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 61440) } returns 53247
                CodePointGenerator(random).randomUtf8(Utf8Level.ThreeByte) shouldBe CodePoint(0xD7FF)
            }

            "ThreeByte - index 53248 picks U+E000 (first after surrogates, skip)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 61440) } returns 53248
                CodePointGenerator(random).randomUtf8(Utf8Level.ThreeByte) shouldBe CodePoint(0xE000)
            }

            "ThreeByte - index 61439 picks U+FFFF (BMP max, last of 3-byte range)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 61440) } returns 61439
                CodePointGenerator(random).randomUtf8(Utf8Level.ThreeByte) shouldBe CodePoint(0xFFFF)
            }

            "FourByte - picks from [0x10000, 0x110000) = 4-byte UTF-8 (supplementary plane)" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x1F600
                CodePointGenerator(random).randomUtf8(Utf8Level.FourByte) shouldBe CodePoint(0x1F600)
            }

            "FourByte - low boundary 0x10000" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x10000
                CodePointGenerator(random).randomUtf8(Utf8Level.FourByte) shouldBe CodePoint(0x10000)
            }

            "FourByte - high boundary 0x10FFFF" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x10FFFF
                CodePointGenerator(random).randomUtf8(Utf8Level.FourByte) shouldBe CodePoint(0x10FFFF)
            }
        }

        "randomUtf16" - {
            // Bmp: 63488 non-surrogate code points indexed virtually:
            // - indices 0..55295    -> U+0000..U+D7FF (before surrogates)
            // - indices 55296..63487 -> U+E000..U+FFFF (after surrogates)

            "Bmp - index 0 picks U+0000 (NUL, start of BMP)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 0
                CodePointGenerator(random).randomUtf16(Utf16Level.Bmp) shouldBe CodePoint(0x0000)
            }

            "Bmp - index 55295 picks U+D7FF (last before surrogates)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 55295
                CodePointGenerator(random).randomUtf16(Utf16Level.Bmp) shouldBe CodePoint(0xD7FF)
            }

            "Bmp - index 55296 picks U+E000 (first after surrogates, skip)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 55296
                CodePointGenerator(random).randomUtf16(Utf16Level.Bmp) shouldBe CodePoint(0xE000)
            }

            "Bmp - index 63487 picks U+FFFF (BMP max)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 63487
                CodePointGenerator(random).randomUtf16(Utf16Level.Bmp) shouldBe CodePoint(0xFFFF)
            }

            "Supplementary - picks from [0x10000, 0x110000) = supplementary plane" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x1F600
                CodePointGenerator(random).randomUtf16(Utf16Level.Supplementary) shouldBe CodePoint(0x1F600)
            }

            "Supplementary - low boundary 0x10000" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x10000
                CodePointGenerator(random).randomUtf16(Utf16Level.Supplementary) shouldBe CodePoint(0x10000)
            }

            "Supplementary - high boundary 0x10FFFF" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x10FFFF
                CodePointGenerator(random).randomUtf16(Utf16Level.Supplementary) shouldBe CodePoint(0x10FFFF)
            }
        }

        "randomUtf32" - {
            // Same backing logic as randomUtf16 (Bmp picks from non-surrogate BMP,
            // Supplementary picks from 0x10000..0x10FFFF). Tests below verify the
            // delegation rather than the underlying math (covered by randomUtf16 tests).

            "Bmp - index 0 picks U+0000 (start of BMP)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 0
                CodePointGenerator(random).randomUtf32(Utf32Level.Bmp) shouldBe CodePoint(0x0000)
            }

            "Bmp - index 55295 picks U+D7FF (last before surrogates)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 55295
                CodePointGenerator(random).randomUtf32(Utf32Level.Bmp) shouldBe CodePoint(0xD7FF)
            }

            "Bmp - index 55296 picks U+E000 (first after surrogates, skip)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 55296
                CodePointGenerator(random).randomUtf32(Utf32Level.Bmp) shouldBe CodePoint(0xE000)
            }

            "Bmp - index 63487 picks U+FFFF (BMP max)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 63488) } returns 63487
                CodePointGenerator(random).randomUtf32(Utf32Level.Bmp) shouldBe CodePoint(0xFFFF)
            }

            "Supplementary - picks from [0x10000, 0x110000)" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x1F600
                CodePointGenerator(random).randomUtf32(Utf32Level.Supplementary) shouldBe CodePoint(0x1F600)
            }

            "Supplementary - low boundary 0x10000" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x10000
                CodePointGenerator(random).randomUtf32(Utf32Level.Supplementary) shouldBe CodePoint(0x10000)
            }

            "Supplementary - high boundary 0x10FFFF" {
                val random = mockk<Random>()
                every { random.nextInt(0x10000, 0x110000) } returns 0x10FFFF
                CodePointGenerator(random).randomUtf32(Utf32Level.Supplementary) shouldBe CodePoint(0x10FFFF)
            }
        }
    })
