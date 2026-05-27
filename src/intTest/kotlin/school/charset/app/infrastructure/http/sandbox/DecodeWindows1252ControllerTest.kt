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
class DecodeWindows1252ControllerTest(
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
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "41"))
            .andExpect(status().isOk)
    }

    @Test
    fun `decodes byte 0x41 (ASCII identity) to U+0041 'A'`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "41"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bytes[0]").value(0x41))
            .andExpect(jsonPath("$.codepoint").value(0x41))
            .andExpect(jsonPath("$.codepointLabel").value("U+0041"))
            .andExpect(jsonPath("$.glyph").value("A"))
            .andExpect(jsonPath("$.steps[0].type").value("binary"))
            .andExpect(jsonPath("$.steps[0].value").value("01000001"))
            .andExpect(jsonPath("$.steps[1].type").value("code-point"))
            .andExpect(jsonPath("$.steps[1].value").value(0x41))
    }

    @Test
    fun `decodes byte 0xE9 (Latin-1 identity) to U+00E9 'é'`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "E9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.codepointLabel").value("U+00E9"))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.steps[0].value").value("11101001"))
            .andExpect(jsonPath("$.steps[1].value").value(0xE9))
    }

    @Test
    fun `decodes byte 0x80 (Microsoft extension) to U+20AC '€'`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "80"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x20AC))
            .andExpect(jsonPath("$.codepointLabel").value("U+20AC"))
            .andExpect(jsonPath("$.glyph").value("€"))
            .andExpect(jsonPath("$.steps[0].value").value("10000000"))
            .andExpect(jsonPath("$.steps[1].value").value(0x20AC))
    }

    @Test
    fun `decodes byte 0x97 (Microsoft extension) to U+2014 em dash`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "97"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x2014))
            .andExpect(jsonPath("$.steps[1].value").value(0x2014))
    }

    @Test
    fun `decodes byte 0x99 (Microsoft extension) to U+2122 trademark`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "99"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x2122))
    }

    @Test
    fun `accepts 0x-prefixed bytes`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "0x80"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x20AC))
    }

    @Test
    fun `returns 422 when byte 0x81 is unassigned`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "81"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when byte 0x8D is unassigned`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "8D"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when byte 0x8F is unassigned`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "8F"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when byte 0x90 is unassigned`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "90"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when byte 0x9D is unassigned`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "9D"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when input is multi-byte (Windows-1252 is 1 byte per code point)`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "41 42"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when bytes input is empty`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when hex is invalid`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "zz"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("invalid_hex"))
    }

    @Test
    fun `returns 422 when hex has odd length`() {
        mockMvc.perform(get("/api/sandbox/decode/windows-1252").param("bytes", "8"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("odd_length"))
    }
}
