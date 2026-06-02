package school.charset.app.infrastructure.http.exercise

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import school.charset.app.config.ApplicationConfigTest
import tools.jackson.databind.json.JsonMapper
import kotlin.uuid.Uuid

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApplicationConfigTest::class)
@Testcontainers
class ExerciseControllerTest(
    private val mockMvc: MockMvc,
    private val mapper: JsonMapper,
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
    fun `POST exercise generate without token_id returns 400`() {
        // The token_id cookie is minted at the Nuxt edge; a request without it
        // is a contract violation.
        mockMvc.perform(
            post("/api/exercise/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"moduleId":"utf8-encode"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorType").value("token-id.missing"))
    }

    @Test
    fun `POST exercise generate returns the exercise without expected values`() {
        generate(aToken(), moduleId = "utf8-encode")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attemptId").isNumber)
            .andExpect(jsonPath("$.moduleId").value("utf8-encode"))
            .andExpect(jsonPath("$.direction").value("encode"))
            .andExpect(jsonPath("$.codePoint").isNumber)
            .andExpect(jsonPath("$.steps[*].expected").doesNotExist())
            .andExpect(jsonPath("$.steps[*].value").doesNotExist())
            .andExpect(jsonPath("$.steps[*].bytes").doesNotExist())
            .andExpect(jsonPath("$.bytes").doesNotExist())
    }

    @Test
    fun `decode module exposes bytes input but hides expected code point`() {
        generate(aToken(), moduleId = "utf8-decode")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.direction").value("decode"))
            .andExpect(jsonPath("$.bytes").isArray)
            .andExpect(jsonPath("$.codePoint").doesNotExist())
            .andExpect(jsonPath("$.steps[*].expected").doesNotExist())
    }

    @Test
    fun `unknown module returns 422`() {
        generate(aToken(), moduleId = "nope-encode")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("exercise.unknown-module"))
    }

    @Test
    fun `validate with wrong answer increments attempts and stays incorrect`() {
        val token = aToken()
        val attempt = generateExerciseJson(token, "utf8-encode")
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(token, attemptId, stepIndex = 0, body = """{"type":"binary","bits":"00000000"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempts").value(1))
            .andExpect(jsonPath("$.canReveal").value(false))
            .andExpect(jsonPath("$.attemptFinalized").value(false))
    }

    @Test
    fun `reveal before threshold returns 422`() {
        val token = aToken()
        val attempt = generateExerciseJson(token, "utf8-encode")
        val attemptId = (attempt["attemptId"] as Number).toLong()

        mockMvc.perform(
            post("/api/exercise/reveal")
                .cookie(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"attemptId":$attemptId,"stepIndex":0}"""),
        ).andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("exercise.reveal-not-allowed"))
    }

    @Test
    fun `validate with missing answer field returns 422 invalid-answer-payload`() {
        val token = aToken()
        val attempt = generateExerciseJson(token, "utf8-encode")
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(token, attemptId, stepIndex = 0, body = """{"type":"binary"}""")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("exercise.invalid-answer-payload"))
    }

    @Test
    fun `attempt is scoped to the token that created it`() {
        val owner = aToken()
        val attempt = generateExerciseJson(owner, "utf8-encode")
        val attemptId = (attempt["attemptId"] as Number).toLong()

        // A different visitor token cannot validate someone else's attempt.
        validate(aToken(), attemptId, stepIndex = 0, body = """{"type":"binary","bits":"00000000"}""")
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorType").value("exercise.attempt-not-found"))
    }

    @Test
    fun `GET current returns null attempt when nothing in progress`() {
        mockMvc.perform(get("/api/exercise/current").param("moduleId", "utf8-encode").cookie(aToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempt").value(null as Any?))
    }

    @Test
    fun `GET current returns the in-progress attempt with stepStates and userAnswer`() {
        val token = aToken()
        val attempt = generateExerciseJson(token, "utf8-encode")
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(token, attemptId, stepIndex = 0, body = """{"type":"format","value":"format-choice.byte-count.2"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(false))

        mockMvc.perform(get("/api/exercise/current").param("moduleId", "utf8-encode").cookie(token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempt.attemptId").value(attemptId))
            .andExpect(jsonPath("$.attempt.stepStates[0].correct").value(false))
            .andExpect(jsonPath("$.attempt.stepStates[0].attempts").value(1))
            .andExpect(jsonPath("$.attempt.stepStates[0].canReveal").value(false))
            .andExpect(jsonPath("$.attempt.stepStates[0].userAnswer.type").value("format"))
            .andExpect(jsonPath("$.attempt.stepStates[0].userAnswer.value").value("format-choice.byte-count.2"))
    }

    @Test
    fun `GET current excludes finalized attempts`() {
        val token = aToken()
        val attempt = generateExerciseJson(token, "utf8-encode")
        val attemptId = (attempt["attemptId"] as Number).toLong()

        @Suppress("UNCHECKED_CAST")
        val steps = attempt["steps"] as List<Map<String, Any?>>
        steps.forEachIndexed { idx, step ->
            val answer = when (step["type"]) {
                "format" -> """{"type":"format","value":"format-choice.byte-count.1"}"""
                "hex-bytes" -> {
                    val bytes = listOf(attempt["codePoint"] as Int)
                    """{"type":"hex-bytes","bytes":$bytes}"""
                }
                else -> error("Unexpected step ${step["type"]}")
            }
            validate(token, attemptId, stepIndex = idx, body = answer)
                .andExpect(status().isOk)
        }

        mockMvc.perform(get("/api/exercise/current").param("moduleId", "utf8-encode").cookie(token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempt").value(null as Any?))
    }

    @Test
    fun `GET progress returns empty list before any exercise`() {
        mockMvc.perform(get("/api/progress").cookie(aToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.progress").isArray)
            .andExpect(jsonPath("$.progress.length()").value(0))
    }

    private fun generate(token: Cookie, moduleId: String): ResultActions = mockMvc.perform(
        post("/api/exercise/generate")
            .cookie(token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(mapOf("moduleId" to moduleId))),
    )

    private fun generateExerciseJson(token: Cookie, moduleId: String): Map<String, Any?> {
        val result = generate(token, moduleId).andReturn()
        @Suppress("UNCHECKED_CAST")
        return mapper.readValue(result.response.contentAsString, Map::class.java) as Map<String, Any?>
    }

    private fun validate(
        token: Cookie,
        attemptId: Long,
        stepIndex: Int,
        body: String,
    ): ResultActions = mockMvc.perform(
        post("/api/exercise/validate")
            .cookie(token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"attemptId":$attemptId,"stepIndex":$stepIndex,"answer":$body}"""),
    )

    // A fresh anonymous-visitor cookie. Each call mints a distinct token, so two
    // calls model two different visitors.
    private fun aToken(): Cookie = Cookie("token_id", Uuid.random().toString())
}
