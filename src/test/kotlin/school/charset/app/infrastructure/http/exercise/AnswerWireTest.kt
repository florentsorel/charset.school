package school.charset.app.infrastructure.http.exercise

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.exercise.Answer

class AnswerWireTest :
    FunSpec({
        test("offset maps to Answer.OffsetValue using the offset field") {
            AnswerWire(type = "offset", offset = 0xAB677).toDomain() shouldBe Answer.OffsetValue(0xAB677)
        }

        test("offset without the offset field is rejected") {
            shouldThrow<InvalidAnswerPayloadException> {
                AnswerWire(type = "offset", offset = null).toDomain()
            }
        }

        test("unknown answer type is rejected") {
            shouldThrow<InvalidAnswerPayloadException> {
                AnswerWire(type = "nope").toDomain()
            }
        }
    })
