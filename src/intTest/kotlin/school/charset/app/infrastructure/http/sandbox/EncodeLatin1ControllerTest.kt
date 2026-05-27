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
class EncodeLatin1ControllerTest(
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
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+0041"))
            .andExpect(status().isOk)
    }

    @Test
    fun `encodes ASCII 'A' (U+0041) as 1-byte 0x41`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+0041"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x41))
            .andExpect(jsonPath("$.codepointLabel").value("U+0041"))
            .andExpect(jsonPath("$.glyph").value("A"))
            .andExpect(jsonPath("$.steps[0].type").value("binary"))
            .andExpect(jsonPath("$.steps[0].value").value("01000001"))
            .andExpect(jsonPath("$.steps[0].length").value(8))
            .andExpect(jsonPath("$.steps[1].type").value("hex-bytes"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0x41))
    }

    @Test
    fun `encodes Latin-1 'é' (U+00E9) as 1-byte 0xE9`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+00E9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.codepointLabel").value("U+00E9"))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.steps[0].value").value("11101001"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0xE9))
    }

    @Test
    fun `encodes boundary U+0000 (NUL) as 0x00 with binary 00000000`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+0000"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0))
            .andExpect(jsonPath("$.label").value("NUL"))
            .andExpect(jsonPath("$.steps[0].value").value("00000000"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0))
    }

    @Test
    fun `encodes boundary U+00FF (ÿ) as 0xFF with binary 11111111`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+00FF"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xFF))
            .andExpect(jsonPath("$.glyph").value("ÿ"))
            .andExpect(jsonPath("$.steps[0].value").value("11111111"))
            .andExpect(jsonPath("$.steps[1].bytes[0]").value(0xFF))
    }

    @Test
    fun `accepts decimal input`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "233"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `accepts 0x-prefixed hex input`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "0xE9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `returns 422 when code point exceeds U+00FF (encoder rejects)`() {
        // U+0100 is the first code point beyond Latin-1. The parser
        // accepts it (it's a valid Unicode code point), the encoder
        // rejects it - so we land on `encoding.not-encodable`, not on
        // `sandbox.input-invalid`.
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+0100"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-encodable"))
            .andExpect(jsonPath("$.params.reason").isString)
    }

    @Test
    fun `returns 422 when code point is far beyond Latin-1 (emoji)`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+1F389"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-encodable"))
    }

    @Test
    fun `returns 422 when input is empty`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when input is unparseable`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "not-a-codepoint"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("unparseable"))
    }

    @Test
    fun `returns 422 when code point is a surrogate (caught by parser before Latin-1)`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+D800"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("surrogate"))
    }

    @Test
    fun `control characters get null glyph and a mnemonic label`() {
        mockMvc.perform(get("/api/sandbox/encode/latin1").param("input", "U+000A")) // LF
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("LF"))
    }
}
