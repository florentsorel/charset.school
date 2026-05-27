package school.charset.app.infrastructure.http.sandbox

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.sandbox.SandboxBytesParser
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
    private val sandboxBytesParser: SandboxBytesParser,
    private val codec: Codec,
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

    @GetMapping("/decode/utf-8")
    fun decodeUtf8(@RequestParam(defaultValue = "") bytes: String): ResponseEntity<Utf8DecodeSandboxResponse> {
        val raw = sandboxBytesParser.parse(bytes)
        val codePoint: CodePoint = codec.decode(raw, Encoding.Utf8)
        val steps = sandboxService.decodeUtf8Verbose(raw)
        return ResponseEntity.ok(
            Utf8DecodeSandboxResponse(
                bytes = raw.map { it.toInt() and 0xFF },
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
     * glyph would be invisible / absent (controls, whitespace, PUA,
     * format chars, combining marks, non-characters, unassigned slots).
     *
     * Delegates the "is this code point displayable?" decision to
     * `CodePointLabels.lookup`: a non-null label means the front renders
     * the label instead of an empty/tofu glyph.
     */
    private fun glyphOf(value: Int): String? {
        if (CodePointLabels.lookup(value) != null) return null
        return String(Character.toChars(value))
    }
}
