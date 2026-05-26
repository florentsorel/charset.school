package school.charset.app.infrastructure.http.sandbox

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.sandbox.SandboxInputParser
import school.charset.app.domain.sandbox.SandboxService

@RestController
@RequestMapping(
    "/api/sandbox",
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SandboxController(
    private val sandboxService: SandboxService,
    private val sandboxInputParser: SandboxInputParser,
) {
    @GetMapping("/encode/utf-8")
    fun encodeUtf8(@RequestParam(defaultValue = "") input: String): ResponseEntity<Utf8SandboxResponse> {
        val codePoint = sandboxInputParser.parse(input)
        val steps = sandboxService.encodeUtf8Verbose(codePoint)
        return ResponseEntity.ok(
            Utf8SandboxResponse(
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                steps = steps,
            ),
        )
    }

    /**
     * Returns the printable glyph for the code point, or `null` when the
     * glyph would be invisible / non-visual:
     *   - C0 controls (0x00-0x1F), DEL (0x7F), C1 controls (0x80-0x9F)
     *   - U+0020 SPACE (renders as a literal blank)
     *
     * For these cases `CodePointLabels.lookup` returns a human-readable
     * label that the front displays in place of the empty glyph.
     */
    private fun glyphOf(value: Int): String? {
        if (value < 0x21 || value in 0x7F..0x9F) return null
        return String(Character.toChars(value))
    }
}
