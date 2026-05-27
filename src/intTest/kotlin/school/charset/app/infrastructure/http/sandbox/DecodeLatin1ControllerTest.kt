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
class DecodeLatin1ControllerTest(
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
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "41"))
            .andExpect(status().isOk)
    }

    @Test
    fun `decodes 1-byte ASCII (0x41) to U+0041 'A'`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "41"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bytes[0]").value(0x41))
            .andExpect(jsonPath("$.codepoint").value(0x41))
            .andExpect(jsonPath("$.codepointLabel").value("U+0041"))
            .andExpect(jsonPath("$.glyph").value("A"))
            .andExpect(jsonPath("$.steps[0].type").value("binary"))
            .andExpect(jsonPath("$.steps[0].value").value("01000001"))
            .andExpect(jsonPath("$.steps[0].length").value(8))
            .andExpect(jsonPath("$.steps[1].type").value("code-point"))
            .andExpect(jsonPath("$.steps[1].value").value(0x41))
    }

    @Test
    fun `decodes Latin-1 0xE9 to U+00E9 'é'`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "E9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bytes[0]").value(0xE9))
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.codepointLabel").value("U+00E9"))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.steps[0].value").value("11101001"))
            .andExpect(jsonPath("$.steps[1].value").value(0xE9))
    }

    @Test
    fun `decodes boundary 0x00 to U+0000 (NUL) with binary 00000000`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "00"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0))
            .andExpect(jsonPath("$.label").value("NUL"))
            .andExpect(jsonPath("$.steps[0].value").value("00000000"))
    }

    @Test
    fun `decodes boundary 0xFF to U+00FF (ÿ) with binary 11111111`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "FF"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xFF))
            .andExpect(jsonPath("$.glyph").value("ÿ"))
            .andExpect(jsonPath("$.steps[0].value").value("11111111"))
    }

    @Test
    fun `accepts 0x-prefixed byte`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "0xE9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `returns 422 when bytes input is empty`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when hex is invalid`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "hello"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("invalid_hex"))
    }

    @Test
    fun `returns 422 when hex has odd length`() {
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "E"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("odd_length"))
    }

    @Test
    fun `returns 422 when more than 1 byte is provided`() {
        // Latin-1 is fixed at 1 byte per code point; the shared parser
        // would accept up to 4 bytes (for UTF-8 / UTF-16), so the
        // controller has its own narrower bound for this endpoint.
        mockMvc.perform(get("/api/sandbox/decode/latin1").param("bytes", "C3 A9"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("too_long"))
    }
}
