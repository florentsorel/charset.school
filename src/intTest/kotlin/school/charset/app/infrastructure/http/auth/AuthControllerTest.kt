package school.charset.app.infrastructure.http.auth

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import school.charset.app.config.ApplicationConfigTest
import school.charset.app.domain.auth.AuthErrorType
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApplicationConfigTest::class)
@Testcontainers
class AuthControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
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
    fun `POST register returns 201 with the created user`() {
        val email = uniqueEmail()
        register(email = email, password = "password123", name = "John", locale = "fr")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.locale").value("fr"))
            .andExpect(jsonPath("$.id").isNumber)
    }

    @Test
    fun `POST register returns 409 when email is already taken`() {
        val email = uniqueEmail()
        register(email = email).andExpect(status().isCreated)

        register(email = email)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorType").value(AuthErrorType.EMAIL_ALREADY_TAKEN))
            .andExpect(jsonPath("$.params.email").value(email))
    }

    @Test
    fun `POST register returns 422 when password is too short`() {
        register(password = "short")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
    }

    @Test
    fun `POST register returns 422 when email is malformed`() {
        register(email = "not-an-email")
            .andExpect(status().isUnprocessableContent)
    }

    @Test
    fun `POST login returns 200 with user info and sets a session cookie`() {
        val email = uniqueEmail()
        register(email = email, password = "password123", name = "Alice", locale = "fr")

        login(email = email, password = "password123")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(cookie().exists("SESSION"))
    }

    @Test
    fun `POST login returns 401 when password is wrong`() {
        val email = uniqueEmail()
        register(email = email, password = "password123")

        login(email = email, password = "wrong-password")
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorType").value(AuthErrorType.BAD_CREDENTIALS))
    }

    @Test
    fun `POST login returns 401 when email does not exist`() {
        login(email = "nobody@example.com", password = "password123")
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorType").value(AuthErrorType.BAD_CREDENTIALS))
    }

    @Test
    fun `POST login with rememberMe sets a remember-me cookie`() {
        val email = uniqueEmail()
        register(email = email, password = "password123")

        login(email = email, password = "password123", rememberMe = true)
            .andExpect(status().isOk)
            .andExpect(cookie().exists("remember-me"))
    }

    @Test
    fun `GET me returns 401 when not authenticated`() {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET me returns 200 with the current user when authenticated`() {
        val email = uniqueEmail()
        register(email = email, password = "password123", name = "Bob", locale = "en")
        val loginResult = login(email = email, password = "password123")
            .andExpect(status().isOk)
            .andReturn()
        val sessionCookie = loginResult.response.getCookie("SESSION")!!

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value("Bob"))
            .andExpect(jsonPath("$.locale").value("en"))
    }

    @Test
    fun `POST logout invalidates the session`() {
        val email = uniqueEmail()
        register(email = email, password = "password123")
        val loginResult: MvcResult = login(email = email, password = "password123").andReturn()
        val sessionCookie = loginResult.response.getCookie("SESSION")!!
        // Real browser flow: read XSRF-TOKEN from login response (set there by
        // our CsrfCookieFilter) and echo it back as cookie + header. `with(csrf())`
        // doesn't add the cookie, so it wouldn't match the verbatim resolveCsrfTokenValue.
        val xsrfCookie = loginResult.response.getCookie("XSRF-TOKEN")!!

        mockMvc.perform(
            post("/api/auth/logout")
                .cookie(sessionCookie, xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.value),
        ).andExpect(status().isNoContent)

        // Same cookie now invalid → 401
        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
            .andExpect(status().isUnauthorized)
    }

    private fun register(
        email: String = uniqueEmail(),
        password: String = "password123",
        name: String = "Test User",
        locale: String = "fr",
    ): ResultActions {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "email" to email,
                "password" to password,
                "name" to name,
                "locale" to locale,
            ),
        )
        return mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
    }

    private fun login(
        email: String,
        password: String,
        rememberMe: Boolean = false,
    ): ResultActions {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "email" to email,
                "password" to password,
                "rememberMe" to rememberMe,
            ),
        )
        return mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
    }

    private fun uniqueEmail(): String = "user-${UUID.randomUUID()}@example.com"
}
