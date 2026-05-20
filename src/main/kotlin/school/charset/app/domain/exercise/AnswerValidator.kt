package school.charset.app.domain.exercise

class AnswerValidator {
    fun validate(step: Step, answer: Answer): ValidationResult = when (step) {
        is Step.Binary -> validateBinary(step, answer)
        is Step.Format -> validateFormat(step, answer)
        is Step.BitGroups -> validateBitGroups(step, answer)
        is Step.HexBytes -> validateHexBytes(step, answer)
        is Step.CodePointEntry -> validateCodePoint(step, answer)
        is Step.Endianness -> validateEndianness(step, answer)
    }

    private fun validateBinary(step: Step.Binary, answer: Answer): ValidationResult {
        if (answer !is Answer.BinaryValue) return typeMismatch(step, answer)

        // Pedagogical order: alphabet first, then length, then value.
        return when {
            answer.bits.isEmpty() -> ValidationResult.incorrect("binary.empty")

            answer.bits.any { it != '0' && it != '1' } -> ValidationResult.incorrect(
                errorType = "binary.invalid-character",
                params = mapOf("got" to answer.bits),
            )

            answer.bits.length < step.length -> ValidationResult.incorrect(
                errorType = "binary.too-few-bits",
                params = mapOf(
                    "expected-length" to step.length.toString(),
                    "got-length" to answer.bits.length.toString(),
                ),
            )

            answer.bits.length > step.length -> ValidationResult.incorrect(
                errorType = "binary.too-many-bits",
                params = mapOf(
                    "expected-length" to step.length.toString(),
                    "got-length" to answer.bits.length.toString(),
                ),
            )

            answer.bits != step.expected -> ValidationResult.incorrect(
                errorType = "binary.wrong-value",
                params = mapOf("got" to answer.bits),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateFormat(step: Step.Format, answer: Answer): ValidationResult {
        if (answer !is Answer.FormatChoice) return typeMismatch(step, answer)

        return when {
            answer.value.isEmpty() -> ValidationResult.incorrect("format.empty")

            // The choices list is public (already shown in the UI) — safe to echo back.
            answer.value !in step.choices -> ValidationResult.incorrect(
                errorType = "format.unknown-choice",
                params = mapOf(
                    "got" to answer.value,
                    "choices" to step.choices.joinToString(", "),
                ),
            )

            answer.value != step.expected -> ValidationResult.incorrect(
                errorType = "format.wrong-choice",
                params = mapOf("got" to answer.value),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateBitGroups(step: Step.BitGroups, answer: Answer): ValidationResult {
        if (answer !is Answer.BitGroupsValue) return typeMismatch(step, answer)

        if (answer.groups.isEmpty()) {
            return ValidationResult.incorrect("bit-groups.empty")
        }

        if (answer.groups.size != step.expected.size) {
            return ValidationResult.incorrect(
                errorType = "bit-groups.wrong-group-count",
                params = mapOf(
                    "expected-count" to step.expected.size.toString(),
                    "got-count" to answer.groups.size.toString(),
                ),
            )
        }

        // Per-group: alphabet first, then length (structural hints — no expected value leak).
        for ((index, group) in answer.groups.withIndex()) {
            val expectedLength = step.expected[index].length
            if (group.any { it != '0' && it != '1' }) {
                return ValidationResult.incorrect(
                    errorType = "bit-groups.invalid-character",
                    params = mapOf("position" to index.toString(), "got" to group),
                )
            }
            if (group.length != expectedLength) {
                return ValidationResult.incorrect(
                    errorType = "bit-groups.wrong-group-length",
                    params = mapOf(
                        "position" to index.toString(),
                        "expected-length" to expectedLength.toString(),
                        "got-length" to group.length.toString(),
                    ),
                )
            }
        }

        return if (answer.groups != step.expected) {
            ValidationResult.incorrect(
                errorType = "bit-groups.wrong-value",
                params = mapOf("got" to answer.groups.joinToString(" ")),
            )
        } else {
            ValidationResult.correct()
        }
    }

    private fun validateHexBytes(step: Step.HexBytes, answer: Answer): ValidationResult {
        if (answer !is Answer.HexBytesValue) return typeMismatch(step, answer)

        if (answer.bytes.isEmpty()) {
            return ValidationResult.incorrect("hex-bytes.empty")
        }

        // Any byte must fit in an octet: 0..255 unsigned.
        val invalidIndex = answer.bytes.indexOfFirst { it !in 0..0xFF }
        if (invalidIndex >= 0) {
            return ValidationResult.incorrect(
                errorType = "hex-bytes.byte-out-of-range",
                params = mapOf(
                    "position" to invalidIndex.toString(),
                    "got" to answer.bytes[invalidIndex].toString(),
                ),
            )
        }

        return when {
            answer.bytes.size < step.expected.size -> ValidationResult.incorrect(
                errorType = "hex-bytes.too-few-bytes",
                params = mapOf(
                    "expected-count" to step.expected.size.toString(),
                    "got-count" to answer.bytes.size.toString(),
                ),
            )

            answer.bytes.size > step.expected.size -> ValidationResult.incorrect(
                errorType = "hex-bytes.too-many-bytes",
                params = mapOf(
                    "expected-count" to step.expected.size.toString(),
                    "got-count" to answer.bytes.size.toString(),
                ),
            )

            answer.bytes != step.expected -> ValidationResult.incorrect(
                errorType = "hex-bytes.wrong-value",
                params = mapOf("got" to answer.bytes.joinToString(" ") { "%02X".format(it) }),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateCodePoint(step: Step.CodePointEntry, answer: Answer): ValidationResult {
        if (answer !is Answer.CodePointValue) return typeMismatch(step, answer)

        return when {
            // Unicode range is public info, not the answer — safe to echo bounds.
            answer.value !in 0..0x10FFFF -> ValidationResult.incorrect(
                errorType = "code-point.out-of-range",
                params = mapOf(
                    "got" to answer.value.toString(),
                    "min" to "0",
                    "max" to "0x10FFFF",
                ),
            )

            answer.value in 0xD800..0xDFFF -> ValidationResult.incorrect(
                errorType = "code-point.surrogate",
                params = mapOf("got" to "U+%04X".format(answer.value)),
            )

            answer.value != step.expected -> ValidationResult.incorrect(
                errorType = "code-point.wrong-value",
                params = mapOf("got" to "U+%04X".format(answer.value)),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateEndianness(step: Step.Endianness, answer: Answer): ValidationResult {
        if (answer !is Answer.EndiannessChoice) return typeMismatch(step, answer)

        // With only two possible values, revealing "got" implicitly reveals the answer.
        // The errorType alone is the feedback — no params needed.
        return if (answer.value != step.expected) {
            ValidationResult.incorrect("endianness.wrong-choice")
        } else {
            ValidationResult.correct()
        }
    }

    private fun typeMismatch(step: Step, answer: Answer): ValidationResult =
        ValidationResult.incorrect(
            errorType = "answer.type-mismatch",
            params = mapOf(
                "expected-type" to step.type.id,
                "got-type" to answer::class.simpleName.orEmpty(),
            ),
        )
}
