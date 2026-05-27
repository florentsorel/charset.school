package school.charset.app.infrastructure.http.sandbox

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import school.charset.app.config.ApplicationConfigTest

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApplicationConfigTest::class)
@Testcontainers
class EncodeWindows1252ControllerTest(
    private val mockMvc: MockMvc,
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
    fun `is publicly accessible without auth`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+0041"))
            .andExpect(status().isOk)
    }

    @Test
    fun `encodes ASCII 'A' (U+0041) as 1-byte 0x41`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+0041"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x41))
            .andExpect(jsonPath("$.codepointLabel").value("U+0041"))
            .andExpect(jsonPath("$.glyph").value("A"))
            .andExpect(jsonPath("$.steps[0].type").value("binary"))
            .andExpect(jsonPath("$.steps[0].value").value("01000001"))
            .andExpect(jsonPath("$.steps[1].type").value("hex-bytes"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0x41))
    }

    @Test
    fun `encodes Latin-1 identity 'é' (U+00E9) as 1-byte 0xE9`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+00E9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.steps[0].value").value("11101001"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0xE9))
    }

    @Test
    fun `encodes Euro '€' (U+20AC) as 1-byte 0x80 - Microsoft extension`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+20AC"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x20AC))
            .andExpect(jsonPath("$.codepointLabel").value("U+20AC"))
            .andExpect(jsonPath("$.glyph").value("€"))
            .andExpect(jsonPath("$.steps[0].value").value("10000000"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0x80))
    }

    @Test
    fun `encodes em dash (U+2014) as 1-byte 0x97 - Microsoft extension`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+2014"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x2014))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0x97))
    }

    @Test
    fun `encodes trademark (U+2122) as 1-byte 0x99 - Microsoft extension`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+2122"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x2122))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0x99))
    }

    @Test
    fun `encodes left smart quote (U+2018) as 1-byte 0x91 - Microsoft extension`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+2018"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0x91))
    }

    @Test
    fun `accepts the character itself as input`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "€"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x20AC))
    }

    @Test
    fun `accepts decimal input`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "233"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `returns 422 when code point is not representable in Windows-1252 (U+0100)`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+0100"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-encodable"))
    }

    @Test
    fun `returns 422 when code point is in 0x80-0x9F unassigned slot (U+0081 maps to no byte)`() {
        // U+0081 is not in the CP1252 table (byte 0x81 is unassigned).
        // Codec.toWindows1252 rejects it because it falls in the special
        // block but has no mapping.
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+0081"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-encodable"))
    }

    @Test
    fun `returns 422 when input is empty`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when input is unparseable`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "not-a-codepoint"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("unparseable"))
    }

    @Test
    fun `returns 422 when code point is a surrogate`() {
        mockMvc.perform(get("/api/sandbox/encode/windows-1252").param("input", "U+D800"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("surrogate"))
    }
}
