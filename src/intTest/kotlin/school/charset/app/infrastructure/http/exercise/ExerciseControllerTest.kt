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
import org.springframework.test.web.servlet.MvcResult
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
    fun `POST exercise generate returns 401 without auth`() {
        val bootstrap: MvcResult = mockMvc.perform(get("/api/auth/me")).andReturn()
        val xsrfCookie = bootstrap.response.getCookie("XSRF-TOKEN")!!

        mockMvc.perform(
            post("/api/exercise/generate")
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"moduleId":"utf8-encode","level":1,"granularity":"verbose"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST exercise generate returns the exercise without expected values`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        generate(sessionCookie, xsrfCookie, moduleId = "utf8-encode", level = 1)
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
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        generate(sessionCookie, xsrfCookie, moduleId = "utf8-decode", level = 1)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.direction").value("decode"))
            .andExpect(jsonPath("$.bytes").isArray)
            .andExpect(jsonPath("$.codePoint").doesNotExist())
            .andExpect(jsonPath("$.steps[*].expected").doesNotExist())
    }

    @Test
    fun `unknown module returns 422`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        generate(sessionCookie, xsrfCookie, moduleId = "nope-encode", level = 1)
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("exercise.unknown-module"))
    }

    @Test
    fun `validate with wrong answer increments attempts and stays incorrect`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val attempt = generateExerciseJson(sessionCookie, xsrfCookie, "utf8-encode", level = 1)
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(sessionCookie, xsrfCookie, attemptId, stepIndex = 0, body = """{"type":"binary","bits":"00000000"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempts").value(1))
            .andExpect(jsonPath("$.canReveal").value(false))
            .andExpect(jsonPath("$.attemptFinalized").value(false))
    }

    @Test
    fun `reveal before threshold returns 422`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val attempt = generateExerciseJson(sessionCookie, xsrfCookie, "utf8-encode", level = 1)
        val attemptId = (attempt["attemptId"] as Number).toLong()

        mockMvc.perform(
            post("/api/exercise/reveal")
                .cookie(sessionCookie, xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"attemptId":$attemptId,"stepIndex":0}"""),
        ).andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("exercise.reveal-not-allowed"))
    }

    @Test
    fun `validate with missing answer field returns 422 invalid-answer-payload`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val attempt = generateExerciseJson(sessionCookie, xsrfCookie, "utf8-encode", level = 1)
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(sessionCookie, xsrfCookie, attemptId, stepIndex = 0, body = """{"type":"binary"}""")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("exercise.invalid-answer-payload"))
    }

    @Test
    fun `multi-byte UTF-8 encode generates Format then Binary then UsefulBitCount then BitGroups then HexBytes`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        generate(sessionCookie, xsrfCookie, moduleId = "utf8-encode", level = 2)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.steps.length()").value(5))
            .andExpect(jsonPath("$.steps[0].type").value("format"))
            .andExpect(jsonPath("$.steps[1].type").value("binary"))
            .andExpect(jsonPath("$.steps[1].length").value(16))
            .andExpect(jsonPath("$.steps[2].type").value("useful-bit-count"))
            .andExpect(jsonPath("$.steps[3].type").value("bit-groups"))
            .andExpect(jsonPath("$.steps[4].type").value("hex-bytes"))
    }

    @Test
    fun `GET current returns null attempt when nothing in progress`() {
        val (sessionCookie, _) = registerAndLogin()

        mockMvc.perform(get("/api/exercise/current").param("moduleId", "utf8-encode").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempt").value(null as Any?))
    }

    @Test
    fun `GET current returns the in-progress attempt with stepStates and userAnswer`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val attempt = generateExerciseJson(sessionCookie, xsrfCookie, "utf8-encode", level = 1)
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(sessionCookie, xsrfCookie, attemptId, stepIndex = 0, body = """{"type":"format","value":"format-choice.byte-count.2"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(false))

        mockMvc.perform(get("/api/exercise/current").param("moduleId", "utf8-encode").cookie(sessionCookie))
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
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val attempt = generateExerciseJson(sessionCookie, xsrfCookie, "utf8-encode", level = 1)
        val attemptId = (attempt["attemptId"] as Number).toLong()

        @Suppress("UNCHECKED_CAST")
        val steps = attempt["steps"] as List<Map<String, Any?>>
        steps.forEachIndexed { idx, step ->
            val answer = when (step["type"]) {
                "format" -> """{"type":"format","value":"format-choice.byte-count.1"}"""
                "hex-bytes" -> {
                    @Suppress("UNCHECKED_CAST")
                    val bytes = listOf(attempt["codePoint"] as Int)
                    """{"type":"hex-bytes","bytes":$bytes}"""
                }
                else -> error("Unexpected step ${step["type"]}")
            }
            validate(sessionCookie, xsrfCookie, attemptId, stepIndex = idx, body = answer)
                .andExpect(status().isOk)
        }

        mockMvc.perform(get("/api/exercise/current").param("moduleId", "utf8-encode").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attempt").value(null as Any?))
    }

    @Test
    fun `validate useful-bit-count step accepts correct count and rejects wrong count`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val attempt = generateExerciseJson(sessionCookie, xsrfCookie, "utf8-encode", level = 2)
        val attemptId = (attempt["attemptId"] as Number).toLong()

        validate(sessionCookie, xsrfCookie, attemptId, stepIndex = 2, body = """{"type":"useful-bit-count","count":7}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.errorType").value("useful-bit-count.wrong-value"))

        validate(sessionCookie, xsrfCookie, attemptId, stepIndex = 2, body = """{"type":"useful-bit-count","count":11}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(true))
    }

    @Test
    fun `GET progress returns empty list before any exercise`() {
        val (sessionCookie, _) = registerAndLogin()

        mockMvc.perform(get("/api/progress").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.progress").isArray)
            .andExpect(jsonPath("$.progress.length()").value(0))
    }

    private fun generate(
        sessionCookie: Cookie,
        xsrfCookie: Cookie,
        moduleId: String,
        level: Int,
        granularity: String = "verbose",
    ): ResultActions = mockMvc.perform(
        post("/api/exercise/generate")
            .cookie(sessionCookie, xsrfCookie)
            .header("X-XSRF-TOKEN", xsrfCookie.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                mapper.writeValueAsString(
                    mapOf("moduleId" to moduleId, "level" to level, "granularity" to granularity),
                ),
            ),
    )

    private fun generateExerciseJson(
        sessionCookie: Cookie,
        xsrfCookie: Cookie,
        moduleId: String,
        level: Int,
    ): Map<String, Any?> {
        val result = generate(sessionCookie, xsrfCookie, moduleId, level).andReturn()
        @Suppress("UNCHECKED_CAST")
        return mapper.readValue(result.response.contentAsString, Map::class.java) as Map<String, Any?>
    }

    private fun validate(
        sessionCookie: Cookie,
        xsrfCookie: Cookie,
        attemptId: Long,
        stepIndex: Int,
        body: String,
    ): ResultActions = mockMvc.perform(
        post("/api/exercise/validate")
            .cookie(sessionCookie, xsrfCookie)
            .header("X-XSRF-TOKEN", xsrfCookie.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"attemptId":$attemptId,"stepIndex":$stepIndex,"answer":$body}"""),
    )

    private fun registerAndLogin(): Pair<Cookie, Cookie> {
        val email = "exercise-${Uuid.random()}@example.test"
        val body = mapper.writeValueAsString(
            mapOf("email" to email, "password" to "password123", "name" to "Tester", "locale" to "fr"),
        )
        val result: MvcResult = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        ).andReturn()
        return Pair(
            result.response.getCookie("SESSION")!!,
            result.response.getCookie("XSRF-TOKEN")!!,
        )
    }
}
