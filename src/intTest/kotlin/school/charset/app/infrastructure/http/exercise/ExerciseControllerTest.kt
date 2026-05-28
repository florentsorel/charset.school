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
