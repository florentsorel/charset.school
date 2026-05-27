package school.charset.app.infrastructure.http.sandbox.serde

import school.charset.app.domain.exercise.Step
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

/**
 * Sandbox wire shape for a domain `Step`. The sandbox is a visualisation
 * tool (not an exercise), so the value carried in `expected` is
 * intentionally exposed - renamed to a context-appropriate field name on
 * the wire:
 *
 *   - `Step.Format`         -> {type:"format", choices:[...], value:expected}
 *   - `Step.Binary`         -> {type:"binary", value:expected, length}
 *   - `Step.BitGroups`      -> {type:"bit-groups", groups:expected}
 *   - `Step.HexBytes`       -> {type:"hex-bytes", bytes:expected}
 *   - `Step.CodePointEntry` -> {type:"code-point", value:expected}
 *
 * `Step.Endianness` is not produced by the UTF-8 encode/decode flows. If
 * it ever reaches this serializer the wire shape is undefined, so we
 * raise `UnsupportedSandboxStepException` (an `IllegalStateException`
 * subtype) - this is an invariant violation, not a business error, and
 * falls through Spring's default 500 handling. No dedicated
 * `@ExceptionHandler` is registered because the situation should never
 * happen at runtime.
 */
class StepSerializer : ValueSerializer<Step>() {
    override fun serialize(step: Step, gen: JsonGenerator, ctx: SerializationContext) {
        gen.writeStartObject()
        gen.writeStringProperty("type", step.type.id)
        when (step) {
            is Step.Format -> {
                gen.writeArrayPropertyStart("choices")
                step.choices.forEach { gen.writeString(it) }
                gen.writeEndArray()
                gen.writeStringProperty("value", step.expected)
            }

            is Step.Binary -> {
                gen.writeStringProperty("value", step.expected)
                gen.writeNumberProperty("length", step.length)
            }

            is Step.BitGroups -> {
                gen.writeArrayPropertyStart("groups")
                step.expected.forEach { gen.writeString(it) }
                gen.writeEndArray()
            }

            is Step.HexBytes -> {
                gen.writeArrayPropertyStart("bytes")
                step.expected.forEach { gen.writeNumber(it) }
                gen.writeEndArray()
            }

            is Step.CodePointEntry -> {
                gen.writeNumberProperty("value", step.expected)
            }

            is Step.Endianness -> throw UnsupportedSandboxStepException(step.type)
        }
        gen.writeEndObject()
    }

    override fun handledType(): Class<Step> = Step::class.java
}
