package school.charset.app

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import school.charset.app.config.DataSourceConfig
import javax.sql.DataSource

@SpringBootTest(classes = [DataSourceConfig::class])
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Testcontainers
class DatabaseConnectionTest(
    private val dataSource: DataSource,
    private val database: Database,
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
    fun `dataSource bean is wired from app datasource properties`() {
        shouldNotThrowAny {
            dataSource.connection.use { it.isValid(1) shouldBe true }
        }
    }

    @Test
    fun `Exposed Database can execute a simple SELECT`() {
        val result = transaction(database) {
            exec("SELECT 1 AS result") { rs ->
                rs.next()
                rs.getInt("result")
            }
        }
        result shouldBe 1
    }

    @Test
    fun `Flyway has run the users migration on startup`() {
        val usersTableExists = transaction(database) {
            exec(
                """
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = 'users'
                ) AS present
                """.trimIndent(),
            ) { rs ->
                rs.next()
                rs.getBoolean("present")
            }
        }
        usersTableExists shouldBe true
    }
}
