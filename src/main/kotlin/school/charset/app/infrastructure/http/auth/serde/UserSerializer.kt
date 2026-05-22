package school.charset.app.infrastructure.http.auth.serde

import school.charset.app.domain.user.User
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

class UserSerializer : ValueSerializer<User>() {
    override fun serialize(user: User, gen: JsonGenerator, ctx: SerializationContext) {
        gen.writeStartObject()
        gen.writeNumberProperty("id", user.id)
        gen.writeStringProperty("email", user.email)
        gen.writeStringProperty("name", user.name)
        gen.writeStringProperty("locale", user.locale)
        gen.writeStringProperty("createdAt", user.createdAt.toString())
        gen.writeEndObject()
    }

    override fun handledType(): Class<User> = User::class.java
}
