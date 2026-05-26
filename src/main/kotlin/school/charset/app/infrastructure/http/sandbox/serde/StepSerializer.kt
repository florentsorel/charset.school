package school.charset.app.infrastructure.http.sandbox.serde

import school.charset.app.domain.exercise.Step
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

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

            is Step.CodePointEntry,
            is Step.Endianness,
            -> throw UnsupportedSandboxStepException(step.type)
        }
        gen.writeEndObject()
    }

    override fun handledType(): Class<Step> = Step::class.java
}
