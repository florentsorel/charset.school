package school.charset.app.infrastructure.http.sandbox

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.sandbox.SandboxBytesParser
import school.charset.app.domain.sandbox.SandboxEndianParser
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
    private val sandboxEndianParser: SandboxEndianParser,
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
        val codePoint = codec.decode(raw, Encoding.Utf8)
        val steps = sandboxService.decodeUtf8Verbose(raw, codePoint)
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

    @GetMapping("/encode/utf-16")
    fun encodeUtf16(
        @RequestParam(defaultValue = "") input: String,
        @RequestParam(defaultValue = "little") endian: String,
    ): ResponseEntity<Utf16EncodeSandboxResponse> {
        val codePoint = sandboxInputParser.parse(input)
        val parsedEndian = sandboxEndianParser.parse(endian)
        val steps = sandboxService.encodeUtf16Verbose(codePoint, parsedEndian)
        return ResponseEntity.ok(
            Utf16EncodeSandboxResponse(
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                endian = parsedEndian.wireValue(),
                steps = steps,
            ),
        )
    }

    @GetMapping("/decode/utf-16")
    fun decodeUtf16(
        @RequestParam(defaultValue = "") bytes: String,
        @RequestParam(defaultValue = "little") endian: String,
    ): ResponseEntity<Utf16DecodeSandboxResponse> {
        val raw = sandboxBytesParser.parse(bytes)
        val parsedEndian = sandboxEndianParser.parse(endian)
        val utf16Encoding = when (parsedEndian) {
            Encoding.Endian.BigEndian -> Encoding.Utf16Be
            Encoding.Endian.LittleEndian -> Encoding.Utf16Le
        }
        val codePoint = codec.decode(raw, utf16Encoding)
        val steps = sandboxService.decodeUtf16Verbose(raw, codePoint, parsedEndian)
        return ResponseEntity.ok(
            Utf16DecodeSandboxResponse(
                bytes = raw.map { it.toInt() and 0xFF },
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                endian = parsedEndian.wireValue(),
                steps = steps,
            ),
        )
    }

    @GetMapping("/encode/utf-32")
    fun encodeUtf32(
        @RequestParam(defaultValue = "") input: String,
        @RequestParam(defaultValue = "little") endian: String,
    ): ResponseEntity<Utf32EncodeSandboxResponse> {
        val codePoint = sandboxInputParser.parse(input)
        val parsedEndian = sandboxEndianParser.parse(endian)
        val steps = sandboxService.encodeUtf32Verbose(codePoint, parsedEndian)
        return ResponseEntity.ok(
            Utf32EncodeSandboxResponse(
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                endian = parsedEndian.wireValue(),
                steps = steps,
            ),
        )
    }

    @GetMapping("/decode/utf-32")
    fun decodeUtf32(
        @RequestParam(defaultValue = "") bytes: String,
        @RequestParam(defaultValue = "little") endian: String,
    ): ResponseEntity<Utf32DecodeSandboxResponse> {
        val raw = sandboxBytesParser.parse(bytes)
        val parsedEndian = sandboxEndianParser.parse(endian)
        val utf32Encoding = when (parsedEndian) {
            Encoding.Endian.BigEndian -> Encoding.Utf32Be
            Encoding.Endian.LittleEndian -> Encoding.Utf32Le
        }
        val codePoint = codec.decode(raw, utf32Encoding)
        val steps = sandboxService.decodeUtf32Verbose(raw, codePoint, parsedEndian)
        return ResponseEntity.ok(
            Utf32DecodeSandboxResponse(
                bytes = raw.map { it.toInt() and 0xFF },
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                endian = parsedEndian.wireValue(),
                steps = steps,
            ),
        )
    }

    @GetMapping("/encode/windows-1252")
    fun encodeWindows1252(@RequestParam(defaultValue = "") input: String): ResponseEntity<Windows1252EncodeSandboxResponse> {
        val codePoint = sandboxInputParser.parse(input)
        // Encode upfront so an unencodable code point (e.g. U+0100 not in
        // the CP1252 table) raises EncoderException now and bubbles up to
        // a 422 `encoding.not-encodable` response, instead of leaking
        // through to step generation.
        codec.encode(codePoint, Encoding.Windows1252)
        val steps = sandboxService.encodeWindows1252Verbose(codePoint)
        return ResponseEntity.ok(
            Windows1252EncodeSandboxResponse(
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                steps = steps,
            ),
        )
    }

    @GetMapping("/decode/windows-1252")
    fun decodeWindows1252(@RequestParam(defaultValue = "") bytes: String): ResponseEntity<Windows1252DecodeSandboxResponse> {
        val raw = sandboxBytesParser.parse(bytes)
        // Decode here so multi-byte inputs and unassigned bytes (0x81, 0x8D,
        // 0x8F, 0x90, 0x9D) raise DecoderException, surfacing as a 422
        // `encoding.not-decodable` response.
        val codePoint = codec.decode(raw, Encoding.Windows1252)
        val steps = sandboxService.decodeWindows1252Verbose(raw, codePoint)
        return ResponseEntity.ok(
            Windows1252DecodeSandboxResponse(
                bytes = raw.map { it.toInt() and 0xFF },
                codepoint = codePoint.value,
                codepointLabel = codePoint.toString(),
                glyph = glyphOf(codePoint.value),
                label = CodePointLabels.lookup(codePoint.value),
                steps = steps,
            ),
        )
    }

    private fun Encoding.Endian.wireValue(): String = when (this) {
        Encoding.Endian.BigEndian -> "big"
        Encoding.Endian.LittleEndian -> "little"
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
