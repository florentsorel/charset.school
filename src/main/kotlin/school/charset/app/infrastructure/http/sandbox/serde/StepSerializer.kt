package school.charset.app.infrastructure.http.sandbox.serde

import school.charset.app.domain.encoding.Encoding
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
 *   - `Step.Endianness`     -> {type:"endianness", value:"big"|"little"}
 *
 * All UTF-8 / UTF-16 / UTF-32 sandbox flows are covered. If a future Step
 * subtype is added without a wire mapping here it will fail-fast at runtime
 * via the `error()` branch in `when` (exhaustive over sealed `Step`).
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

            is Step.UsefulBitCount -> {
                gen.writeNumberProperty("value", step.expected)
            }

            is Step.Endianness -> {
                gen.writeStringProperty(
                    "value",
                    when (step.expected) {
                        Encoding.Endian.BigEndian -> "big"
                        Encoding.Endian.LittleEndian -> "little"
                    },
                )
            }

            // Exercise-only step (the sandbox explains the subtraction in prose);
            // mapped here to satisfy the exhaustive `when` over sealed Step.
            is Step.Offset -> {
                gen.writeNumberProperty("value", step.expected)
            }
        }
        gen.writeEndObject()
    }

    override fun handledType(): Class<Step> = Step::class.java
}
