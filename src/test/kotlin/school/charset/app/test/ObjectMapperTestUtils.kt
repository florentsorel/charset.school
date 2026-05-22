package school.charset.app.test

import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule

@Suppress("UNCHECKED_CAST")
object ObjectMapperTestUtils {
    /**
     * Builds a fresh [JsonMapper] with the given serializer registered.
     * [ValueSerializer.handledType] must be implemented on the serializer.
     */
    fun <T> JsonMapper.withSerializer(serializer: ValueSerializer<T>): JsonMapper {
        val module = SimpleModule().apply { addSerializer(serializer) }
        return this.rebuild().addModule(module).build()
    }

    /**
     * Builds a fresh [JsonMapper] with the given deserializer registered.
     * [ValueDeserializer.handledType] must be implemented on the deserializer.
     */
    fun <T> JsonMapper.withDeserializer(deserializer: ValueDeserializer<T>): JsonMapper {
        val module = SimpleModule().apply {
            addDeserializer(deserializer.handledType() as Class<T>, deserializer)
        }
        return this.rebuild().addModule(module).build()
    }
}
