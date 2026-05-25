package school.charset.app.infrastructure.repository.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import school.charset.app.config.ApplicationConfigTest
import school.charset.app.config.DatabaseConfig
import school.charset.app.config.UserConfig
import school.charset.app.domain.user.EmailAlreadyTakenException
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.UserRepository
import java.util.UUID

@SpringBootTest(
    classes = [DatabaseConfig::class, UserConfig::class, ApplicationConfigTest::class],
)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Testcontainers
class ExposedUserRepositoryTest(
    private val userRepository: UserRepository,
) {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine")

        @JvmStatic
        @DynamicPropertySource
        @Suppress("unused")
        fun dataSourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("app.datasource.url") { postgres.jdbcUrl }
            registry.add("app.datasource.username") { postgres.username }
            registry.add("app.datasource.password") { postgres.password }
            registry.add("app.datasource.driver-class-name") { postgres.driverClassName }
        }
    }

    @Test
    fun `create persists the user and returns the generated id`() {
        val email = uniqueEmail()
        val hash = PasswordHash("hash")
        val user = userRepository.create(
            email = email,
            name = "John",
            passwordHash = hash,
            locale = "fr",
        )

        (user.id > 0L) shouldBe true
        user.email shouldBe email
        user.name shouldBe "John"
        user.passwordHash shouldBe hash
        user.locale shouldBe "fr"
        user.updatedAt.shouldBeNull()
    }

    @Test
    fun `findById returns the persisted user`() {
        val created = userRepository.create(
            email = uniqueEmail(),
            name = "Alice",
            passwordHash = PasswordHash("h"),
            locale = "fr",
        )
        userRepository.findById(created.id) shouldBe created
    }

    @Test
    fun `findById returns null when id does not exist`() {
        userRepository.findById(Long.MAX_VALUE).shouldBeNull()
    }

    @Test
    fun `findByEmail returns the persisted user`() {
        val email = uniqueEmail()
        val created = userRepository.create(
            email = email,
            name = "Bob",
            passwordHash = PasswordHash("h"),
            locale = "en",
        )
        userRepository.findByEmail(email) shouldBe created
    }

    @Test
    fun `findByEmail returns null when email does not exist`() {
        userRepository.findByEmail("nobody@example.com").shouldBeNull()
    }

    @Test
    fun `create throws EmailAlreadyTakenException when email is already taken`() {
        val email = uniqueEmail()
        userRepository.create(email = email, name = "Eve", passwordHash = PasswordHash("h"), locale = "fr")
        val ex = shouldThrow<EmailAlreadyTakenException> {
            userRepository.create(email = email, name = "Eve", passwordHash = PasswordHash("h"), locale = "fr")
        }
        ex.email shouldBe email
    }

    @Test
    fun `update applies the given fields and sets updatedAt`() {
        val created = userRepository.create(
            email = uniqueEmail(),
            name = "Before",
            passwordHash = PasswordHash("h"),
            locale = "fr",
        )

        val updated = userRepository.update(
            id = created.id,
            name = "After",
            locale = "en",
        )

        updated.id shouldBe created.id
        updated.name shouldBe "After"
        updated.locale shouldBe "en"
        updated.email shouldBe created.email
        updated.passwordHash shouldBe created.passwordHash
        (updated.updatedAt != null) shouldBe true
    }

    @Test
    fun `update with all null fields only bumps updatedAt`() {
        val created = userRepository.create(
            email = uniqueEmail(),
            name = "Stay",
            passwordHash = PasswordHash("h"),
            locale = "fr",
        )

        val updated = userRepository.update(id = created.id)

        updated.name shouldBe "Stay"
        updated.email shouldBe created.email
        updated.locale shouldBe "fr"
        (updated.updatedAt != null) shouldBe true
    }

    @Test
    fun `update throws EmailAlreadyTakenException when email belongs to another user`() {
        val taken = uniqueEmail()
        userRepository.create(email = taken, name = "Owner", passwordHash = PasswordHash("h"), locale = "fr")
        val other = userRepository.create(email = uniqueEmail(), name = "Other", passwordHash = PasswordHash("h"), locale = "fr")

        val ex = shouldThrow<EmailAlreadyTakenException> {
            userRepository.update(id = other.id, email = taken)
        }
        ex.email shouldBe taken
    }

    @Test
    fun `updatePasswordHash replaces the hash and bumps updatedAt`() {
        val created = userRepository.create(
            email = uniqueEmail(),
            name = "Bob",
            passwordHash = PasswordHash("old-hash"),
            locale = "fr",
        )

        val updated = userRepository.updatePasswordHash(created.id, PasswordHash("new-hash"))

        updated.passwordHash shouldBe PasswordHash("new-hash")
        updated.email shouldBe created.email
        updated.name shouldBe created.name
        (updated.updatedAt != null) shouldBe true
    }

    private fun uniqueEmail(): String = "user-${UUID.randomUUID()}@example.com"
}
