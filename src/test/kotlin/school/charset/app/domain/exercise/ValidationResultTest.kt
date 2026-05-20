package school.charset.app.domain.exercise

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ValidationResultTest :
    FreeSpec({
        "correct()" - {
            "produces ok=true, no errorType, no params" {
                val result = ValidationResult.correct()
                result.ok shouldBe true
                result.errorType shouldBe null
                result.params shouldBe emptyMap()
            }
        }

        "incorrect()" - {
            "with errorType only produces ok=false and empty params" {
                val result = ValidationResult.incorrect(errorType = "some.error")
                result.ok shouldBe false
                result.errorType shouldBe "some.error"
                result.params shouldBe emptyMap()
            }

            "with errorType and params" {
                val result = ValidationResult.incorrect(
                    errorType = "some.error",
                    params = mapOf("got" to "x"),
                )
                result.ok shouldBe false
                result.errorType shouldBe "some.error"
                result.params shouldBe mapOf("got" to "x")
            }
        }

        "invariants" - {
            // The primary constructor is private, but reflection or a sloppy refactor
            // could still produce invalid states. These tests pin the runtime guarantee.

            "incorrect with empty errorType is impossible via factory" {
                // incorrect() requires a non-null errorType; missing it is a compile error.
                // Smoke test only: factory accepts a string.
                ValidationResult.incorrect("anything").ok shouldBe false
            }
        }

        // Equality / hashCode behavior - needed because validator tests compare via shouldBe.
        "equality" - {
            "two correct() results are equal" {
                ValidationResult.correct() shouldBe ValidationResult.correct()
            }

            "two incorrect() with same errorType and params are equal" {
                ValidationResult.incorrect("a", mapOf("k" to "v")) shouldBe
                    ValidationResult.incorrect("a", mapOf("k" to "v"))
            }

            "correct() != incorrect()" {
                (ValidationResult.correct() == ValidationResult.incorrect("a")) shouldBe false
            }
        }
    })
