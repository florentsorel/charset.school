package school.charset.app.infrastructure.http.exercise.serde

import school.charset.app.infrastructure.http.exercise.ExerciseStepDto
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

class ExerciseStepDtoSerializer : ValueSerializer<ExerciseStepDto>() {
    override fun serialize(step: ExerciseStepDto, gen: JsonGenerator, ctx: SerializationContext) {
        gen.writeStartObject()
        gen.writeStringProperty("type", step.type)
        when (step) {
            is ExerciseStepDto.Format -> {
                gen.writeArrayPropertyStart("choices")
                step.choices.forEach { gen.writeString(it) }
                gen.writeEndArray()
            }
            is ExerciseStepDto.Binary -> gen.writeNumberProperty("length", step.length)
            is ExerciseStepDto.BitGroups -> {
                gen.writeArrayPropertyStart("groupLengths")
                step.groupLengths.forEach { gen.writeNumber(it) }
                gen.writeEndArray()
            }
            is ExerciseStepDto.HexBytes -> gen.writeNumberProperty("byteCount", step.byteCount)
            ExerciseStepDto.CodePointEntry,
            ExerciseStepDto.UsefulBitCount,
            ExerciseStepDto.Endianness,
            ExerciseStepDto.Offset,
            -> {
            }
        }
        gen.writeEndObject()
    }

    override fun handledType(): Class<ExerciseStepDto> = ExerciseStepDto::class.java
}
