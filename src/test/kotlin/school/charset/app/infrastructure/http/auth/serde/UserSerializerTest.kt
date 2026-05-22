package school.charset.app.infrastructure.http.auth.serde

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.User
import school.charset.app.test.ObjectMapperTestUtils.withSerializer
import tools.jackson.databind.json.JsonMapper
import kotlin.time.Instant

class UserSerializerTest :
    FunSpec({
        val mapper = JsonMapper().withSerializer(UserSerializer())

        fun aUser(
            id: Long = 1L,
            email: String = "user@example.com",
            name: String = "User",
            passwordHash: PasswordHash = PasswordHash("hash"),
            locale: String = "fr",
            createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt: Instant? = null,
        ): User = User(id, email, name, passwordHash, locale, createdAt, updatedAt)

        fun User.serializeAndAssert(expectedJson: String) {
            val actual = mapper.writeValueAsString(this)
            mapper.readTree(actual) shouldBe mapper.readTree(expectedJson)
        }

        test("serializes id, email, name, locale and createdAt") {
            aUser(
                id = 42L,
                email = "john@example.com",
                name = "John",
                locale = "fr",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ).serializeAndAssert(
                """
                {
                    "id": 42,
                    "email": "john@example.com",
                    "name": "John",
                    "locale": "fr",
                    "createdAt": "2026-01-01T00:00:00Z"
                }
                """,
            )
        }

        test("never exposes passwordHash or updatedAt") {
            aUser(
                id = 7L,
                email = "alice@example.com",
                name = "Alice",
                passwordHash = PasswordHash("super-secret-hash"),
                locale = "en",
                createdAt = Instant.parse("2026-03-15T10:00:00Z"),
                updatedAt = Instant.parse("2026-03-15T11:00:00Z"),
            ).serializeAndAssert(
                """
                {
                    "id": 7,
                    "email": "alice@example.com",
                    "name": "Alice",
                    "locale": "en",
                    "createdAt": "2026-03-15T10:00:00Z"
                }
                """,
            )
        }
    })
