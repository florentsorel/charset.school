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
class DecodeUtf32ControllerTest(
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
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 00 00 41")
                .param("endian", "big"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `decodes 4-byte BigEndian 00 00 00 E9 to U+00E9 'é'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 00 00 E9")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.codepointLabel").value("U+00E9"))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.endian").value("big"))
            .andExpect(jsonPath("$.steps[0].type").value("endianness"))
            .andExpect(jsonPath("$.steps[0].value").value("big"))
            .andExpect(jsonPath("$.steps[1].type").value("binary"))
            .andExpect(jsonPath("$.steps[1].value").value("00000000000000000000000011101001"))
            .andExpect(jsonPath("$.steps[1].length").value(32))
            .andExpect(jsonPath("$.steps[2].type").value("code-point"))
            .andExpect(jsonPath("$.steps[2].value").value(0xE9))
    }

    @Test
    fun `decodes 4-byte LittleEndian E9 00 00 00 to U+00E9 'é'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "E9 00 00 00")
                .param("endian", "little"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.endian").value("little"))
    }

    @Test
    fun `decodes 4-byte BigEndian 00 01 F3 89 to U+1F389 'tada'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 01 F3 89")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x1F389))
            .andExpect(jsonPath("$.codepointLabel").value("U+1F389"))
            .andExpect(jsonPath("$.steps[1].value").value("00000000000000011111001110001001"))
            .andExpect(jsonPath("$.steps[2].value").value(0x1F389))
    }

    @Test
    fun `decodes 4-byte LittleEndian 89 F3 01 00 to U+1F389 'tada'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "89 F3 01 00")
                .param("endian", "little"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x1F389))
    }

    @Test
    fun `defaults endian to 'little' when not provided (matches real-world Windows usage)`() {
        // Little-endian decode of `E9 00 00 00` -> U+00E9 'é'.
        mockMvc.perform(get("/api/sandbox/decode/utf-32").param("bytes", "E9 00 00 00"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.endian").value("little"))
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `returns 422 when endian param is invalid`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 00 00 41")
                .param("endian", "middle"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.endian-invalid"))
    }

    @Test
    fun `returns 422 when bytes count is 3 (UTF-32 requires exactly 4 bytes)`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 00 E9")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when bytes count is 2 (UTF-32 requires exactly 4 bytes)`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 E9")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when decoded value exceeds U+10FFFF (out of Unicode range)`() {
        // 0x00 0x11 0x00 0x00 BE = 0x110000, just past the Unicode limit.
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 11 00 00")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when decoded value lands inside the surrogate range`() {
        // 0x00 0x00 0xD8 0x00 BE = 0xD800 (high surrogate), invalid as a code point.
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "00 00 D8 00")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when bytes are empty`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-32")
                .param("bytes", "")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.bytes-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }
}
