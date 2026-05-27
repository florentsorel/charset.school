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
class EncodeUtf32ControllerTest(
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
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+0041")
                .param("endian", "big"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `encodes 'é' (U+00E9) BigEndian as 00 00 00 E9`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+00E9")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0xE9))
            .andExpect(jsonPath("$.endian").value("big"))
            .andExpect(jsonPath("$.steps[0].type").value("endianness"))
            .andExpect(jsonPath("$.steps[0].value").value("big"))
            .andExpect(jsonPath("$.steps[1].type").value("binary"))
            .andExpect(jsonPath("$.steps[1].value").value("00000000000000000000000011101001"))
            .andExpect(jsonPath("$.steps[1].length").value(32))
            .andExpect(jsonPath("$.steps[2].type").value("hex-bytes"))
            .andExpect(jsonPath("$.steps[2].bytes[0]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[1]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[2]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[3]").value(0xE9))
    }

    @Test
    fun `encodes 'é' (U+00E9) LittleEndian as E9 00 00 00`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+00E9")
                .param("endian", "little"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.endian").value("little"))
            .andExpect(jsonPath("$.steps[2].bytes[0]").value(0xE9))
            .andExpect(jsonPath("$.steps[2].bytes[1]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[2]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[3]").value(0x00))
    }

    @Test
    fun `encodes 'tada' (U+1F389) BigEndian as 00 01 F3 89`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+1F389")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x1F389))
            .andExpect(jsonPath("$.steps[1].value").value("00000000000000011111001110001001"))
            .andExpect(jsonPath("$.steps[2].bytes[0]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[1]").value(0x01))
            .andExpect(jsonPath("$.steps[2].bytes[2]").value(0xF3))
            .andExpect(jsonPath("$.steps[2].bytes[3]").value(0x89))
    }

    @Test
    fun `encodes 'tada' (U+1F389) LittleEndian as 89 F3 01 00`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+1F389")
                .param("endian", "little"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.steps[2].bytes[0]").value(0x89))
            .andExpect(jsonPath("$.steps[2].bytes[1]").value(0xF3))
            .andExpect(jsonPath("$.steps[2].bytes[2]").value(0x01))
            .andExpect(jsonPath("$.steps[2].bytes[3]").value(0x00))
    }

    @Test
    fun `encodes max code point U+10FFFF BigEndian as 00 10 FF FF`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+10FFFF")
                .param("endian", "big"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.codepoint").value(0x10FFFF))
            .andExpect(jsonPath("$.steps[2].bytes[0]").value(0x00))
            .andExpect(jsonPath("$.steps[2].bytes[1]").value(0x10))
            .andExpect(jsonPath("$.steps[2].bytes[2]").value(0xFF))
            .andExpect(jsonPath("$.steps[2].bytes[3]").value(0xFF))
    }

    @Test
    fun `defaults endian to 'little' when not provided (matches real-world Windows usage)`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-32").param("input", "U+00E9"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.endian").value("little"))
    }

    @Test
    fun `returns 422 when endian param is invalid`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+00E9")
                .param("endian", "middle"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.endian-invalid"))
            .andExpect(jsonPath("$.params.reason").value("invalid"))
    }

    @Test
    fun `returns 422 when input is empty`() {
        mockMvc.perform(get("/api/sandbox/encode/utf-32").param("input", ""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("empty"))
    }

    @Test
    fun `returns 422 when code point is a surrogate (unencodable in UTF-32)`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+D800")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("surrogate"))
    }

    @Test
    fun `returns 422 when code point is beyond U+10FFFF (out of Unicode range)`() {
        mockMvc.perform(
            get("/api/sandbox/encode/utf-32")
                .param("input", "U+110000")
                .param("endian", "big"),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.errorType").value("sandbox.input-invalid"))
            .andExpect(jsonPath("$.params.reason").value("out_of_range"))
    }
}
