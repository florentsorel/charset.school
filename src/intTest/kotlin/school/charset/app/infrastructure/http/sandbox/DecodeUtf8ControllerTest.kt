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
class DecodeUtf8ControllerTest(
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
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "41"))
            .andExpect(status().isOk)
    }

    @Test
    fun `decodes 1-byte ASCII (0x41) to U+0041 'A'`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "41"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bytes[0]").value(0x41))
            .andExpect(jsonPath("$.codepoint").value(0x41))
            .andExpect(jsonPath("$.codepointLabel").value("U+0041"))
            .andExpect(jsonPath("$.glyph").value("A"))
    }

    @Test
    fun `decodes 2-byte 0xC3 0xA9 to U+00E9 'é'`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "C3 A9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.codepointLabel").value("U+00E9"))
            .andExpect(jsonPath("$.glyph").value("é"))
    }

    @Test
    fun `decodes 4-byte 0xF0 0x9F 0x8E 0x89 to U+1F389 '🎉'`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "F0 9F 8E 89"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x1F389))
            .andExpect(jsonPath("$.codepointLabel").value("U+1F389"))
            .andExpect(jsonPath("$.glyph").value("🎉"))
    }

    @Test
    fun `accepts contiguous hex with no separator`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "C3A9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `accepts 0x-prefixed bytes`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "0xC3 0xA9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `response carries step-by-step pedagogical artefacts`() {
        // The "é" path should expose format + bit-groups + binary + code-point steps.
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "C3 A9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.steps[0].type").value("format"))
            .andExpect(jsonPath("$.steps[1].type").value("bit-groups"))
            .andExpect(jsonPath("$.steps[2].type").value("binary"))
            .andExpect(jsonPath("$.steps[3].type").value("code-point"))
            .andExpect(jsonPath("$.steps[3].value").value(0xE9))
    }

    @Test
    fun `returns 422 when bytes input is empty`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when hex is invalid`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "hello"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("invalid_hex"))
    }

    @Test
    fun `returns 422 when hex has odd length`() {
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "C3A"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("odd_length"))
    }

    @Test
    fun `returns 422 when bytes are not a valid UTF-8 sequence`() {
        // 0xC3 alone is a 2-byte leader without continuation.
        mockMvc.perform(get("/api/sandbox/decode/utf-8").param("bytes", "C3"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }
}
