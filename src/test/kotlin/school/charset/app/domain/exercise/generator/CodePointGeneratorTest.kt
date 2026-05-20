package school.charset.app.domain.exercise.generator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.ExerciseGenerationException
import kotlin.random.Random

class CodePointGeneratorTest :
    FreeSpec({
        // Random is mocked with MockK. Each test pins both the bounds passed to
        // `Random.nextInt(from, until)` (via `every` with explicit arguments) and
        // the value returned. If the production code passes wrong bounds, MockK
        // throws "no mocking found" and the test fails fast.

        "randomAscii" - {
            "level 1 — picks from [0x20, 0x7F) = printable ASCII" {
                val random = mockk<Random>()
                every { random.nextInt(0x20, 0x7F) } returns 0x41
                CodePointGenerator(random).randomAscii(level = 1) shouldBe CodePoint(0x41)
            }

            "level 1 — low boundary 0x20" {
                val random = mockk<Random>()
                every { random.nextInt(0x20, 0x7F) } returns 0x20
                CodePointGenerator(random).randomAscii(level = 1) shouldBe CodePoint(0x20)
            }

            "level 1 — high boundary 0x7E" {
                val random = mockk<Random>()
                every { random.nextInt(0x20, 0x7F) } returns 0x7E
                CodePointGenerator(random).randomAscii(level = 1) shouldBe CodePoint(0x7E)
            }

            "level 2 — picks from [0x00, 0x80) = full ASCII including controls" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x41
                CodePointGenerator(random).randomAscii(level = 2) shouldBe CodePoint(0x41)
            }

            "level 2 — low boundary 0x00 (NUL)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x00
                CodePointGenerator(random).randomAscii(level = 2) shouldBe CodePoint(0x00)
            }

            "level 2 — high boundary 0x7F (DEL)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x80) } returns 0x7F
                CodePointGenerator(random).randomAscii(level = 2) shouldBe CodePoint(0x7F)
            }

            "invalid level throws ExerciseGenerationException" {
                val sut = CodePointGenerator(mockk())
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.randomAscii(level = 99)
                }
                exception.encoding shouldBe Encoding.Ascii
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for ascii level 99: level must be 1 or 2"
            }
        }

        "randomLatin1" - {
            "level 1 — picks from [0xA0, 0x100) = Latin-1 supplement only" {
                val random = mockk<Random>()
                every { random.nextInt(0xA0, 0x100) } returns 0xE9
                CodePointGenerator(random).randomLatin1(level = 1) shouldBe CodePoint(0xE9)
            }

            "level 1 — low boundary 0xA0 (NBSP)" {
                val random = mockk<Random>()
                every { random.nextInt(0xA0, 0x100) } returns 0xA0
                CodePointGenerator(random).randomLatin1(level = 1) shouldBe CodePoint(0xA0)
            }

            "level 1 — high boundary 0xFF (ÿ)" {
                val random = mockk<Random>()
                every { random.nextInt(0xA0, 0x100) } returns 0xFF
                CodePointGenerator(random).randomLatin1(level = 1) shouldBe CodePoint(0xFF)
            }

            "level 2 — picks from [0x00, 0x100) = full Latin-1" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x100) } returns 0xE9
                CodePointGenerator(random).randomLatin1(level = 2) shouldBe CodePoint(0xE9)
            }

            "level 2 — low boundary 0x00 (NUL)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x100) } returns 0x00
                CodePointGenerator(random).randomLatin1(level = 2) shouldBe CodePoint(0x00)
            }

            "level 2 — high boundary 0xFF (ÿ)" {
                val random = mockk<Random>()
                every { random.nextInt(0x00, 0x100) } returns 0xFF
                CodePointGenerator(random).randomLatin1(level = 2) shouldBe CodePoint(0xFF)
            }

            "invalid level throws ExerciseGenerationException" {
                val sut = CodePointGenerator(mockk())
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.randomLatin1(level = 99)
                }
                exception.encoding shouldBe Encoding.Latin1
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for latin1 level 99: level must be 1 or 2"
            }
        }

        "randomWindows1252" - {
            // Level 1 picks from the 27 special code points only.
            // Indices are stable: 0 = byte 0x80 (€), ..., 26 = byte 0x9F (Ÿ).

            "level 1 — index 0 picks Euro (U+20AC, byte 0x80)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 27) } returns 0
                CodePointGenerator(random).randomWindows1252(level = 1) shouldBe CodePoint(0x20AC)
            }

            "level 1 — index 11 picks Œ (U+0152, byte 0x8C)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 27) } returns 11
                CodePointGenerator(random).randomWindows1252(level = 1) shouldBe CodePoint(0x0152)
            }

            "level 1 — index 26 picks Ÿ (U+0178, byte 0x9F, last special)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 27) } returns 26
                CodePointGenerator(random).randomWindows1252(level = 1) shouldBe CodePoint(0x0178)
            }

            // Level 2 picks from all 251 encodable code points. Layout:
            // - indices 0..127     : ASCII range (U+0000..U+007F)
            // - indices 128..154   : special block (27 entries, in byte order)
            // - indices 155..250   : Latin-1 supplement (U+00A0..U+00FF)

            "level 2 — index 0 picks U+0000 (NUL, start of ASCII)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 0
                CodePointGenerator(random).randomWindows1252(level = 2) shouldBe CodePoint(0x00)
            }

            "level 2 — index 127 picks U+007F (DEL, end of ASCII)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 127
                CodePointGenerator(random).randomWindows1252(level = 2) shouldBe CodePoint(0x7F)
            }

            "level 2 — index 128 picks U+20AC (Euro, first special)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 128
                CodePointGenerator(random).randomWindows1252(level = 2) shouldBe CodePoint(0x20AC)
            }

            "level 2 — index 154 picks U+0178 (Ÿ, last special)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 154
                CodePointGenerator(random).randomWindows1252(level = 2) shouldBe CodePoint(0x0178)
            }

            "level 2 — index 155 picks U+00A0 (NBSP, start of Latin-1 supplement)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 155
                CodePointGenerator(random).randomWindows1252(level = 2) shouldBe CodePoint(0xA0)
            }

            "level 2 — index 250 picks U+00FF (ÿ, end)" {
                val random = mockk<Random>()
                every { random.nextInt(0, 251) } returns 250
                CodePointGenerator(random).randomWindows1252(level = 2) shouldBe CodePoint(0xFF)
            }

            "invalid level throws ExerciseGenerationException" {
                val sut = CodePointGenerator(mockk())
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.randomWindows1252(level = 99)
                }
                exception.encoding shouldBe Encoding.Windows1252
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for windows-1252 level 99: level must be 1 or 2"
            }
        }
    })
