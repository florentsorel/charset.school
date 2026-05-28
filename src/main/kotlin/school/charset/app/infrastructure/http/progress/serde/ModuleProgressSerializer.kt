package school.charset.app.infrastructure.http.progress.serde

import school.charset.app.domain.progress.ModuleProgress
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

class ModuleProgressSerializer : ValueSerializer<ModuleProgress>() {
    override fun serialize(progress: ModuleProgress, gen: JsonGenerator, ctx: SerializationContext) {
        gen.writeStartObject()
        gen.writeStringProperty("moduleId", progress.module.id)
        gen.writeNumberProperty("level", progress.level)
        gen.writeNumberProperty("streak", progress.streak)
        gen.writeNumberProperty("attempts", progress.attempts)
        gen.writeNumberProperty("errors", progress.errors)
        gen.writeNumberProperty("suggestedLevel", progress.suggestedLevel)
        if (progress.lastPlayedAt != null) {
            gen.writeStringProperty("lastPlayedAt", progress.lastPlayedAt.toString())
        } else {
            gen.writeNullProperty("lastPlayedAt")
        }
        gen.writeEndObject()
    }

    override fun handledType(): Class<ModuleProgress> = ModuleProgress::class.java
}
