package school.charset.app.domain.exercise

/**
 * Stable identifiers for the options displayed by a `Step.Format` choice.
 *
 * Same convention as `ErrorType` and `ParamKey`: domain-side `const val` strings
 * that the front consumes as i18n keys. The front maps each identifier to a
 * locale-specific display string (e.g., `format-choice.byte-count.2` → "2 octets"
 * in FR, "2 bytes" in EN).
 *
 * The user clicks a button labelled with the translation. The front sends back
 * the **identifier** (not the translation), which `AnswerValidator` compares to
 * `Step.Format.expected`. So the validator logic is agnostic of the locale.
 */
object FormatChoice {
    // Byte count choices used by UTF-8 (1-4 bytes), UTF-16 (2 or 4 bytes),
    // and UTF-32 (always 4 bytes - others are pedagogical "decoys").
    const val ONE_BYTE = "format-choice.byte-count.1"
    const val TWO_BYTES = "format-choice.byte-count.2"
    const val THREE_BYTES = "format-choice.byte-count.3"
    const val FOUR_BYTES = "format-choice.byte-count.4"

    // Code-unit count choices used by UTF-16 (1 or 2 code units). A UTF-16
    // code unit is 16 bits (2 bytes); surrogate pairs span 2 units = 4 bytes.
    const val ONE_CODE_UNIT = "format-choice.code-unit.1"
    const val TWO_CODE_UNITS = "format-choice.code-unit.2"
}
