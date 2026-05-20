package school.charset.app.domain.exercise

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
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
    })
