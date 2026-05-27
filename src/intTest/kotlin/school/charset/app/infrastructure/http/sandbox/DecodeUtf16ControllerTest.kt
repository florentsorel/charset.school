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
class DecodeUtf16ControllerTest(
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
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "00 41")
                .param("endian", "big"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `decodes 2-byte BigEndian 00 E9 to U+00E9 'é'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "00 E9")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.codepointLabel").value("U+00E9"))
            .andExpect(jsonPath("$.glyph").value("é"))
            .andExpect(jsonPath("$.endian").value("big"))
            .andExpect(jsonPath("$.steps[0].type").value("endianness"))
            .andExpect(jsonPath("$.steps[1].type").value("format"))
            .andExpect(jsonPath("$.steps[1].value").value("format-choice.code-unit.1"))
            .andExpect(jsonPath("$.steps[2].type").value("binary"))
            .andExpect(jsonPath("$.steps[3].type").value("code-point"))
            .andExpect(jsonPath("$.steps[3].value").value(0xE9))
    }

    @Test
    fun `decodes 2-byte LittleEndian E9 00 to U+00E9 'é'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "E9 00")
                .param("endian", "little"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.endian").value("little"))
    }

    @Test
    fun `decodes 4-byte surrogate pair D8 3C DF 89 BigEndian to U+1F389 '🎉'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "D8 3C DF 89")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x1F389))
            .andExpect(jsonPath("$.codepointLabel").value("U+1F389"))
            .andExpect(jsonPath("$.glyph").value("🎉"))
            .andExpect(jsonPath("$.steps[1].value").value("format-choice.code-unit.2"))
            .andExpect(jsonPath("$.steps[2].type").value("bit-groups"))
            .andExpect(jsonPath("$.steps[2].groups[0]").value("0000111100"))
            .andExpect(jsonPath("$.steps[2].groups[1]").value("1110001001"))
            .andExpect(jsonPath("$.steps[3].type").value("binary"))
            .andExpect(jsonPath("$.steps[3].value").value("00001111001110001001"))
            .andExpect(jsonPath("$.steps[4].value").value(0x1F389))
    }

    @Test
    fun `decodes 4-byte surrogate pair LittleEndian 3C D8 89 DF to U+1F389 '🎉'`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "3C D8 89 DF")
                .param("endian", "little"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x1F389))
    }

    @Test
    fun `defaults endian to 'little' when not provided (matches real-world Windows usage)`() {
        // Little-endian decode of `E9 00` -> U+00E9 'é'.
        mockMvc.perform(get("/api/sandbox/decode/utf-16").param("bytes", "E9 00"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.endian").value("little"))
            .andExpect(jsonPath("$.codepoint").value(0xE9))
    }

    @Test
    fun `returns 422 when endian param is invalid`() {
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "00 41")
                .param("endian", "middle"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.endian-invalid"))
    }

    @Test
    fun `returns 422 when bytes are not a valid UTF-16 sequence (lone high surrogate)`() {
        // 0xD8 0x3C alone = high surrogate without low surrogate follower
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "D8 3C")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }

    @Test
    fun `returns 422 when bytes count is 3 (not a valid UTF-16 code unit boundary)`() {
        // UTF-16 needs exactly 2 or 4 bytes - Codec rejects 3-byte input.
        mockMvc.perform(
            get("/api/sandbox/decode/utf-16")
                .param("bytes", "00 E9 AA")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("encoding.not-decodable"))
    }
}
