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
class EncodeUtf8ControllerTest(
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
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+0041"))
            .andExpect(status().isOk)
    }

    @Test
    fun `encodes ASCII 'A' (U+0041) as 1-byte 0x41`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+0041"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x41))
            .andExpect(jsonPath("$.codepointLabel").value("U+0041"))
            .andExpect(jsonPath("$.glyph").value("A"))
            .andExpect(jsonPath("$.steps[2].type").value("hex-bytes"))
            .andExpect(jsonPath("$.steps[2].bytes[0]").value(0x41))
    }

    @Test
    fun `encodes 'é' (U+00E9) as 2-byte 0xC3 0xA9`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+00E9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.steps[1].value").value("00011101001"))
            .andExpect(jsonPath("$.steps[2].groups[0]").value("00011"))
            .andExpect(jsonPath("$.steps[2].groups[1]").value("101001"))
            .andExpect(jsonPath("$.steps[3].bytes[0]").value(0xC3))
            .andExpect(jsonPath("$.steps[3].bytes[1]").value(0xA9))
    }

    @Test
    fun `encodes '🎉' (U+1F389) as 4-byte 0xF0 0x9F 0x8E 0x89`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "🎉"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepointLabel").value("U+1F389"))
            .andExpect(jsonPath("$.glyph").value("🎉"))
            .andExpect(jsonPath("$.steps[3].bytes[0]").value(0xF0))
            .andExpect(jsonPath("$.steps[3].bytes[1]").value(0x9F))
            .andExpect(jsonPath("$.steps[3].bytes[2]").value(0x8E))
            .andExpect(jsonPath("$.steps[3].bytes[3]").value(0x89))
    }

    @Test
    fun `accepts decimal input`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "233"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `accepts 0x-prefixed hex input`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "0xE9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `control characters get null glyph and a mnemonic label`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+000A")) // LF
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("LF"))

        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+000F")) // SI
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("SI"))
    }

    @Test
    fun `U+0020 SPACE gets null glyph and label SPACE (so the UI shows something)`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+0020"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("SPACE"))
    }

    @Test
    fun `printable code points get a glyph and null label`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+0041")) // 'A'
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").value("A"))
            .andExpect(jsonPath("$.label").doesNotExist())
    }

    @Test
    fun `private use area gets null glyph and label PUA`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+F389"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("PUA"))
    }

    @Test
    fun `non-character gets null glyph and label NONCHAR`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+FFFE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("NONCHAR"))
    }

    @Test
    fun `named invisible chars get their short mnemonic (BOM, ZWJ)`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+FEFF"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("BOM"))

        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+200D"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("ZWJ"))
    }

    @Test
    fun `combining mark gets null glyph and label COMBINING`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+0301")) // combining acute accent
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.glyph").doesNotExist())
            .andExpect(jsonPath("$.label").value("COMBINING"))
    }

    @Test
    fun `returns 422 when input is empty`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when input is unparseable`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "not-a-codepoint"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("unparseable"))
    }

    @Test
    fun `returns 422 when code point is out of Unicode range`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+110000"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("out_of_range"))
    }

    @Test
    fun `returns 422 when code point is a surrogate`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8").param("input", "U+D800"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("surrogate"))
    }

    @Test
    fun `returns 422 when input param is missing entirely (defaults to empty)`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-8"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }
}
