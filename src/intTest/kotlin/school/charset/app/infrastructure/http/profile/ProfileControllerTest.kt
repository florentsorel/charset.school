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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
import school.charset.app.domain.profile.ProfileValidationKey
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

    @Test
    fun `PATCH profile password updates the hash, accepts the new password on login`() {
        val email = uniqueEmail()
        val (sessionCookie, xsrfCookie) = registerAndLogin(email = email, password = "current-password")

        changePassword(
            sessionCookie,
            xsrfCookie,
            currentPassword = "current-password",
            newPassword = "new-password-123",
        ).andExpect(status().isNoContent)

        login(email = email, password = "new-password-123").andExpect(status().isOk)
        login(email = email, password = "current-password").andExpect(status().isUnauthorized)
    }

    @Test
    fun `PATCH profile password returns 422 when current password is wrong`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(password = "real-password")

        changePassword(
            sessionCookie,
            xsrfCookie,
            currentPassword = "wrong-password",
            newPassword = "new-password-123",
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.currentPassword[0]").value(ProfileValidationKey.CURRENT_PASSWORD_MISMATCH))
    }

    @Test
    fun `PATCH profile password returns 422 when new password is too short`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(password = "current-password")

        changePassword(
            sessionCookie,
            xsrfCookie,
            currentPassword = "current-password",
            newPassword = "short",
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.newPassword").exists())
    }

    @Test
    fun `PATCH profile password returns 422 when current password is blank`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        changePassword(
            sessionCookie,
            xsrfCookie,
            currentPassword = "",
            newPassword = "new-password-123",
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.fieldErrors.currentPassword").exists())
    }

    @Test
    fun `PATCH profile password returns 422 when confirmPassword does not match newPassword`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(password = "current-password")

        changePassword(
            sessionCookie,
            xsrfCookie,
            currentPassword = "current-password",
            newPassword = "new-password-123",
            confirmPassword = "different-confirmation",
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.confirmPassword[0]").value(ProfileValidationKey.PASSWORD_CONFIRM_MISMATCH))
    }

    @Test
    fun `PATCH profile password returns 422 when confirmPassword is blank`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        changePassword(
            sessionCookie,
            xsrfCookie,
            currentPassword = "password123",
            newPassword = "new-password-123",
            confirmPassword = "",
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.fieldErrors.confirmPassword").exists())
    }

    @Test
    fun `PATCH profile password returns 401 when not authenticated`() {
        val bootstrap: MvcResult = mockMvc.perform(get("/api/auth/me")).andReturn()
        val xsrfCookie = bootstrap.response.getCookie("XSRF-TOKEN")!!

        mockMvc.perform(
            patch("/api/profile/password")
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"a","newPassword":"b-very-long"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE profile deletes the account, clears cookies and future logins fail`() {
        val email = uniqueEmail()
        val (sessionCookie, xsrfCookie) = registerAndLogin(email = email, password = "real-password")

        val result = deleteAccount(sessionCookie, xsrfCookie, password = "real-password")
            .andExpect(status().isNoContent)
            .andReturn()

        val clearedSession = result.response.getCookie("SESSION")
        val clearedRememberMe = result.response.getCookie("remember-me")
        // The DELETE response must instruct the browser to drop both cookies.
        check(clearedSession?.maxAge == 0) { "SESSION cookie should be expired" }
        check(clearedRememberMe?.maxAge == 0) { "remember-me cookie should be expired" }

        // Future login attempts return 401 — the user no longer exists.
        login(email = email, password = "real-password").andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE profile returns 422 when password is wrong`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin(password = "real-password")

        deleteAccount(sessionCookie, xsrfCookie, password = "wrong-password")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.password[0]").value(ProfileValidationKey.CURRENT_PASSWORD_MISMATCH))
    }

    @Test
    fun `DELETE profile returns 422 when password is blank`() {
        val (sessionCookie, xsrfCookie) = registerAndLogin()

        deleteAccount(sessionCookie, xsrfCookie, password = "")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("validation.failed"))
            .andExpect(jsonPath("$.fieldErrors.password").exists())
    }

    @Test
    fun `DELETE profile returns 403 when CSRF token is missing`() {
        mockMvc.perform(
            delete("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"x"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE profile returns 401 when not authenticated`() {
        val bootstrap: MvcResult = mockMvc.perform(get("/api/auth/me")).andReturn()
        val xsrfCookie = bootstrap.response.getCookie("XSRF-TOKEN")!!

        mockMvc.perform(
            delete("/api/profile")
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password":"x"}"""),
        ).andExpect(status().isUnauthorized)
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

    private fun changePassword(
        sessionCookie: Cookie,
        xsrfCookie: Cookie,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String = newPassword,
    ): ResultActions = mockMvc.perform(
        patch("/api/profile/password")
            .cookie(sessionCookie, xsrfCookie)
            .header("X-XSRF-TOKEN", xsrfCookie.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                mapper.writeValueAsString(
                    mapOf(
                        "currentPassword" to currentPassword,
                        "newPassword" to newPassword,
                        "confirmPassword" to confirmPassword,
                    ),
                ),
            ),
    )

    private fun deleteAccount(
        sessionCookie: Cookie,
        xsrfCookie: Cookie,
        password: String,
    ): ResultActions = mockMvc.perform(
        delete("/api/profile")
            .cookie(sessionCookie, xsrfCookie)
            .header("X-XSRF-TOKEN", xsrfCookie.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(mapOf("password" to password))),
    )

    private fun login(email: String, password: String): ResultActions = mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(mapOf("email" to email, "password" to password))),
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
