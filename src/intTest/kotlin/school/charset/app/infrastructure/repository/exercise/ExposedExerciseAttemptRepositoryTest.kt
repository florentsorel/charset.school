package school.charset.app.infrastructure.repository.exercise

import io.kotest.matchers.collections.shouldContainExactly
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
import school.charset.app.config.ExerciseConfig
import school.charset.app.config.ProgressConfig
import school.charset.app.config.UserConfig
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer
import school.charset.app.domain.exercise.ExerciseAttemptRepository
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.Granularity
import school.charset.app.domain.exercise.Step
import school.charset.app.domain.user.PasswordHash
import school.charset.app.domain.user.UserRepository
import kotlin.uuid.Uuid

@SpringBootTest(
    classes = [
        DatabaseConfig::class,
        UserConfig::class,
        ExerciseConfig::class,
        ProgressConfig::class,
        ApplicationConfigTest::class,
    ],
)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Testcontainers
class ExposedExerciseAttemptRepositoryTest(
    private val repository: ExerciseAttemptRepository,
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
    fun `create persists attempt steps and child rows and findById returns them`() {
        val userId = createUser()
        val steps = listOf(
            Step.Format(choices = listOf("byte-count.1", "byte-count.2"), expected = "byte-count.2"),
            Step.Binary(expected = "11001000", length = 8),
            Step.HexBytes(expected = listOf(0xC3, 0xA9)),
        )

        val created = repository.create(
            userId = userId,
            module = ExerciseModule.Utf8Encode,
            level = 2,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0xE9),
            encoding = Encoding.Utf8,
            steps = steps,
        )

        val loaded = repository.findById(created.id)!!
        loaded.userId shouldBe userId
        loaded.module shouldBe ExerciseModule.Utf8Encode
        loaded.level shouldBe 2
        loaded.granularity shouldBe Granularity.Verbose
        loaded.codePoint.value shouldBe 0xE9
        loaded.encoding shouldBe Encoding.Utf8
        loaded.correct shouldBe false
        loaded.steps.map { it.step } shouldContainExactly steps
        loaded.steps.forEach { step ->
            step.attempts shouldBe 0
            step.correct shouldBe false
            step.revealed shouldBe false
            step.userAnswer shouldBe null
        }
    }

    @Test
    fun `recordStepSubmission persists user answer, increments attempts and updates correctness`() {
        val userId = createUser()
        val attempt = repository.create(
            userId = userId,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )
        val stepId = attempt.steps[0].id

        repository.recordStepSubmission(
            stepId = stepId,
            userAnswer = Answer.BinaryValue("00000000"),
            correct = false,
            errorType = "binary.wrong-value",
        )
        repository.recordStepSubmission(
            stepId = stepId,
            userAnswer = Answer.BinaryValue("01000001"),
            correct = true,
            errorType = null,
        )

        val reloaded = repository.findById(attempt.id)!!
        val step = reloaded.steps.single()
        step.attempts shouldBe 2
        step.correct shouldBe true
        step.errorType shouldBe null
        step.userAnswer shouldBe Answer.BinaryValue("01000001")
    }

    @Test
    fun `UsefulBitCount step persists and round-trips`() {
        val userId = createUser()
        val attempt = repository.create(
            userId = userId,
            module = ExerciseModule.Utf8Encode,
            level = 2,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0xE9),
            encoding = Encoding.Utf8,
            steps = listOf(Step.UsefulBitCount(expected = 11)),
        )
        val stepId = attempt.steps[0].id

        repository.recordStepSubmission(
            stepId = stepId,
            userAnswer = Answer.UsefulBitCountValue(11),
            correct = true,
            errorType = null,
        )

        val reloaded = repository.findById(attempt.id)!!
        val step = reloaded.steps.single()
        step.step shouldBe Step.UsefulBitCount(expected = 11)
        step.correct shouldBe true
        step.userAnswer shouldBe Answer.UsefulBitCountValue(11)
    }

    @Test
    fun `findLatestUnfinalizedByUserAndModule returns null when no attempt exists`() {
        val userId = createUser()
        repository.findLatestUnfinalizedByUserAndModule(userId, ExerciseModule.Utf8Encode) shouldBe null
    }

    @Test
    fun `findLatestUnfinalizedByUserAndModule returns the in-progress attempt`() {
        val userId = createUser()
        val attempt = repository.create(
            userId = userId,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )

        val found = repository.findLatestUnfinalizedByUserAndModule(userId, ExerciseModule.Utf8Encode)!!
        found.id shouldBe attempt.id
    }

    @Test
    fun `findLatestUnfinalizedByUserAndModule excludes finalized attempts`() {
        val userId = createUser()
        val attempt = repository.create(
            userId = userId,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )
        repository.finalize(attempt.id, correct = true, durationMs = 0)

        repository.findLatestUnfinalizedByUserAndModule(userId, ExerciseModule.Utf8Encode) shouldBe null
    }

    @Test
    fun `findLatestUnfinalizedByUserAndModule scopes by user and module`() {
        val ownerId = createUser()
        val otherId = createUser()
        repository.create(
            userId = ownerId,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )

        repository.findLatestUnfinalizedByUserAndModule(otherId, ExerciseModule.Utf8Encode) shouldBe null
        repository.findLatestUnfinalizedByUserAndModule(ownerId, ExerciseModule.Utf8Decode) shouldBe null
    }

    @Test
    fun `finalize sets correct, finalized and durationMs`() {
        val userId = createUser()
        val attempt = repository.create(
            userId = userId,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            granularity = Granularity.Verbose,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )

        repository.finalize(attempt.id, correct = true, durationMs = 1234)

        val finalized = repository.findById(attempt.id)!!
        finalized.correct shouldBe true
        finalized.finalized shouldBe true
        finalized.durationMs shouldBe 1234
    }

    private fun createUser(): Long = userRepository.create(
        email = "repo-${Uuid.random()}@example.test",
        name = "Tester",
        passwordHash = PasswordHash("hash"),
        locale = "fr",
    ).id
}
