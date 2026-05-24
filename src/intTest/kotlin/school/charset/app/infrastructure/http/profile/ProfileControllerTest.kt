package school.charset.app.infrastructure.http.profile

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import school.charset.app.config.ApplicationConfigTest
import school.charset.app.domain.auth.AuthErrorType
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApplicationConfigTest::class)
@Testcontainers
class ProfileControllerTest(
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
    fun `PATCH profile returns 403 when CSRF token is missing`() {
        mockMvc.perform(
            patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"X"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH profile returns 401 when CSRF is valid but no session`() {
        // Bootstrap an XSRF-TOKEN cookie via a no-auth endpoint (CsrfCookieFilter
        // writes it on every response). Then PATCH with that token but no SESSION
        // — CSRF passes, auth check fails → 401 (not 403).
        val bootstrap: MvcResult = mockMvc.perform(get("/api/auth/me")).andReturn()
        val xsrfCookie = bootstrap.response.getCookie("XSRF-TOKEN")!!

        mockMvc.perform(
            patch("/api/profile")
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"X"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `PATCH profile updates the name`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(name = "Old Name")

        patchProfile(sessionCookie, xsrfCookie, mapOf("name" to "New Name"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
    }

    @Test
    fun `PATCH profile updates the email`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val newEmail = uniqueEmail()

        patchProfile(sessionCookie, xsrfCookie, mapOf("email" to newEmail))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(newEmail))
    }

    @Test
    fun `PATCH profile updates the locale`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(locale = "fr")

        patchProfile(sessionCookie, xsrfCookie, mapOf("locale" to "en"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.locale").value("en"))
    }

    @Test
    fun `PATCH profile updates multiple fields at once`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()
        val newEmail = uniqueEmail()

        patchProfile(
            sessionCookie,
            xsrfCookie,
            mapOf(
                "name" to "Alice",
                "email" to newEmail,
                "locale" to "en",
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value(newEmail))
            .andExpect(jsonPath("$.locale").value("en"))
    }

    @Test
    fun `PATCH profile leaves untouched fields unchanged`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(name = "Keep", locale = "fr")

        patchProfile(sessionCookie, xsrfCookie, mapOf("locale" to "en"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Keep"))
            .andExpect(jsonPath("$.locale").value("en"))
    }

    @Test
    fun `PATCH profile returns 409 when email already used by another user`() {
        val taken = uniqueEmail()
        register(email = taken)

        val (sessionCookie, xsrfCookie) = registerAndLogin()

        patchProfile(sessionCookie, xsrfCookie, mapOf("email" to taken))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorType").value(AuthErrorType.EMAIL_ALREADY_TAKEN))
            .andExpect(jsonPath("$.params.email").value(taken))
    }

    @Test
    fun `PATCH profile returns 422 when email is malformed`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        patchProfile(sessionCookie, xsrfCookie, mapOf("email" to "not-an-email"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
    }

    @Test
    fun `PATCH profile returns 422 when email is empty`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        patchProfile(sessionCookie, xsrfCookie, mapOf("email" to ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.email").exists())
    }

    @Test
    fun `PATCH profile returns 422 when name is empty`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        patchProfile(sessionCookie, xsrfCookie, mapOf("name" to ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.name").exists())
    }

    @Test
    fun `PATCH profile returns 422 when name is only whitespace`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        patchProfile(sessionCookie, xsrfCookie, mapOf("name" to "   "))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.name").exists())
    }

    @Test
    fun `PATCH profile returns 422 when locale is not supported`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        patchProfile(sessionCookie, xsrfCookie, mapOf("locale" to "de"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
    }

    private fun registerAndLogin(
        email: String = uniqueEmail(),
        password: String = "password123",
        name: String = "Test User",
        locale: String = "fr",
    ): Pair<Cookie, Cookie> {
        val registerResult: MvcResult = register(email = email, password = password, name = name, locale = locale).andReturn()
        return Pair(
            registerResult.response.getCookie("SESSION")!!,
            registerResult.response.getCookie("XSRF-TOKEN")!!,
        )
    }

    private fun patchProfile(
        sessionCookie: Cookie,
        xsrfCookie: Cookie,
        body: Map<String, Any?>,
    ): ResultActions = mockMvc.perform(
        patch("/api/profile")
            .cookie(sessionCookie, xsrfCookie)
            .header("X-XSRF-TOKEN", xsrfCookie.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(body)),
    )

    private fun register(
        email: String = uniqueEmail(),
        password: String = "password123",
        name: String = "Test User",
        locale: String = "fr",
    ): ResultActions {
        val body = mapper.writeValueAsString(
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

    private fun uniqueEmail(): String = "user-${UUID.randomUUID()}@example.com"
}
