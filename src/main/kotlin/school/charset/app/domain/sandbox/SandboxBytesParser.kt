package school.charset.app.domain.sandbox

/**
 * Parses the free-form text the sandbox user types to enter a sequence of
 * bytes (for decode flows). Accepted shapes are intentionally generous,
 * any of:
 *   - `C3 A9`         (hex pairs separated by spaces)
 *   - `C3,A9`         (comma-separated)
 *   - `C3-A9`         (dash-separated)
 *   - `C3A9`          (no separator)
 *   - `0xC3 0xA9`     (with hex prefix per byte)
 *   - `0xC3,0xA9`     (combinations)
 *
 * Lives in the domain layer (no Spring) so the validation rules are
 * single-sourced here. The front sends raw text; this class decides if
 * it's a valid byte sequence.
 *
 * Mirrors `SandboxInputParser` for the encode flow.
 */
class SandboxBytesParser {
    private val separators = Regex("[\\s,;-]+")

    /**
     * @throws SandboxBytesParseException if the input is empty, contains
     *   non-hex characters after normalisation, or has an odd number of
     *   hex digits (incomplete final byte).
     */
    fun parse(raw: String): ByteArray {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) throw SandboxBytesParseException(REASON_EMPTY)

        // Strip every `0x` / `0X` prefix (with or without surrounding
        // separators) and every separator, leaving only hex digits.
        val normalized = trimmed
            .replace(Regex("0[xX]"), "")
            .replace(separators, "")

        if (normalized.isEmpty()) throw SandboxBytesParseException(REASON_EMPTY)
        if (!normalized.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            throw SandboxBytesParseException(REASON_INVALID_HEX)
        }
        if (normalized.length % 2 != 0) {
            throw SandboxBytesParseException(REASON_ODD_LENGTH)
        }
        if (normalized.length / 2 > MAX_BYTES) {
            throw SandboxBytesParseException(REASON_TOO_LONG)
        }

        return ByteArray(normalized.length / 2) { i ->
            normalized.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        const val MAX_BYTES = 4
        const val REASON_EMPTY = "empty"
        const val REASON_INVALID_HEX = "invalid_hex"
        const val REASON_ODD_LENGTH = "odd_length"
        const val REASON_TOO_LONG = "too_long"
    }
}

/**
 * Thrown by `SandboxBytesParser.parse` when the user's text input can't
 * be resolved to a byte sequence. `reason` is one of the stable
 * identifiers declared on `SandboxBytesParser.Companion` (front consumes
 * them as i18n keys, so renaming is a wire-breaking change).
 */
class SandboxBytesParseException(val reason: String) : RuntimeException("Failed to parse sandbox bytes input: $reason")
