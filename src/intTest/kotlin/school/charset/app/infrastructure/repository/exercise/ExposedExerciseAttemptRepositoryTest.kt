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
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Answer
import school.charset.app.domain.exercise.ExerciseAttemptRepository
import school.charset.app.domain.exercise.ExerciseModule
import school.charset.app.domain.exercise.Step
import kotlin.uuid.Uuid

@SpringBootTest(
    classes = [
        DatabaseConfig::class,
        ExerciseConfig::class,
        ProgressConfig::class,
        ApplicationConfigTest::class,
    ],
)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Testcontainers
class ExposedExerciseAttemptRepositoryTest(
    private val repository: ExerciseAttemptRepository,
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
        val token = aToken()
        val steps = listOf(
            Step.Format(choices = listOf("byte-count.1", "byte-count.2"), expected = "byte-count.2"),
            Step.Binary(expected = "11001000", length = 8),
            Step.HexBytes(expected = listOf(0xC3, 0xA9)),
        )

        val created = repository.create(
            token = token,
            module = ExerciseModule.Utf8Encode,
            level = 2,
            codePoint = CodePoint(0xE9),
            encoding = Encoding.Utf8,
            steps = steps,
        )

        val loaded = repository.findById(created.id)!!
        loaded.token shouldBe token
        loaded.module shouldBe ExerciseModule.Utf8Encode
        loaded.level shouldBe 2
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
        val token = aToken()
        val attempt = repository.create(
            token = token,
            module = ExerciseModule.Utf8Encode,
            level = 1,
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
        val token = aToken()
        val attempt = repository.create(
            token = token,
            module = ExerciseModule.Utf8Encode,
            level = 2,
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
    fun `Offset step persists and round-trips`() {
        val token = aToken()
        val attempt = repository.create(
            token = token,
            module = ExerciseModule.Utf16Encode,
            level = 2,
            codePoint = CodePoint(0x1F389),
            encoding = Encoding.Utf16Be,
            steps = listOf(Step.Offset(expected = 0xF389)),
        )
        val stepId = attempt.steps[0].id

        repository.recordStepSubmission(
            stepId = stepId,
            userAnswer = Answer.OffsetValue(0xF389),
            correct = true,
            errorType = null,
        )

        val reloaded = repository.findById(attempt.id)!!
        val step = reloaded.steps.single()
        step.step shouldBe Step.Offset(expected = 0xF389)
        step.correct shouldBe true
        step.userAnswer shouldBe Answer.OffsetValue(0xF389)
    }

    @Test
    fun `findLatestUnfinalizedByTokenAndModule returns null when no attempt exists`() {
        repository.findLatestUnfinalizedByTokenAndModule(aToken(), ExerciseModule.Utf8Encode) shouldBe null
    }

    @Test
    fun `findLatestUnfinalizedByTokenAndModule returns the in-progress attempt`() {
        val token = aToken()
        val attempt = repository.create(
            token = token,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )

        val found = repository.findLatestUnfinalizedByTokenAndModule(token, ExerciseModule.Utf8Encode)!!
        found.id shouldBe attempt.id
    }

    @Test
    fun `findLatestUnfinalizedByTokenAndModule excludes finalized attempts`() {
        val token = aToken()
        val attempt = repository.create(
            token = token,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )
        repository.finalize(attempt.id, correct = true, durationMs = 0)

        repository.findLatestUnfinalizedByTokenAndModule(token, ExerciseModule.Utf8Encode) shouldBe null
    }

    @Test
    fun `findLatestUnfinalizedByTokenAndModule scopes by token and module`() {
        val ownerToken = aToken()
        val otherToken = aToken()
        repository.create(
            token = ownerToken,
            module = ExerciseModule.Utf8Encode,
            level = 1,
            codePoint = CodePoint(0x41),
            encoding = Encoding.Utf8,
            steps = listOf(Step.Binary(expected = "01000001", length = 8)),
        )

        repository.findLatestUnfinalizedByTokenAndModule(otherToken, ExerciseModule.Utf8Encode) shouldBe null
        repository.findLatestUnfinalizedByTokenAndModule(ownerToken, ExerciseModule.Utf8Decode) shouldBe null
    }

    @Test
    fun `finalize sets correct, finalized and durationMs`() {
        val token = aToken()
        val attempt = repository.create(
            token = token,
            module = ExerciseModule.Utf8Encode,
            level = 1,
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

    private fun aToken(): String = Uuid.random().toString()
}
