package school.charset.app.domain.exercise

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.Encoding
import kotlin.random.Random

class CodePointGeneratorTest :
    FreeSpec({
        "randomAscii" - {
            "level 1 - printable range" {
                val sut = CodePointGenerator(Random(42))
                repeat(50) {
                    val cp = sut.randomAscii(level = 1)
                    cp.value shouldBeInRange 0x20..0x7E
                }
            }

            "level 2 - full ASCII including control chars" {
                val sut = CodePointGenerator(Random(7))
                repeat(50) {
                    val cp = sut.randomAscii(level = 2)
                    cp.value shouldBeInRange 0x00..0x7F
                }
            }

            "deterministic with same seed" {
                val first = CodePointGenerator(Random(42)).randomAscii(level = 1)
                val second = CodePointGenerator(Random(42)).randomAscii(level = 1)
                first shouldBe second
            }

            "invalid level throws ExerciseGenerationException" {
                val sut = CodePointGenerator(Random(42))
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.randomAscii(level = 99)
                }
                exception.encoding shouldBe Encoding.Ascii
                exception.level shouldBe 99
                exception.message shouldBe "Cannot generate exercise for ascii level 99: level must be 1 or 2"
            }
        }
    })
