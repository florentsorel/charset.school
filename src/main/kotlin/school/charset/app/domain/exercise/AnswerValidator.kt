package school.charset.app.domain.exercise

class AnswerValidator {
    fun validate(step: Step, answer: Answer): ValidationResult = when (step) {
        is Step.Binary -> validateBinary(step, answer)
        is Step.Format -> validateFormat(step, answer)
        is Step.BitGroups -> validateBitGroups(step, answer)
        is Step.HexBytes -> validateHexBytes(step, answer)
        is Step.CodePointEntry -> validateCodePoint(step, answer)
        is Step.UsefulBitCount -> validateUsefulBitCount(step, answer)
        is Step.Endianness -> validateEndianness(step, answer)
        is Step.Offset -> validateOffset(step, answer)
    }

    private fun validateOffset(step: Step.Offset, answer: Answer): ValidationResult {
        if (answer !is Answer.OffsetValue) return typeMismatch(step, answer)

        return when {
            // The 20-bit range is public structure, not the answer - safe to echo bounds.
            answer.value !in 0..0xFFFFF -> ValidationResult.incorrect(
                errorType = ErrorType.Offset.OUT_OF_RANGE,
                params = mapOf(
                    ParamKey.GOT to answer.value.toString(),
                    ParamKey.MIN to "0",
                    ParamKey.MAX to "0xFFFFF",
                ),
            )

            answer.value != step.expected -> ValidationResult.incorrect(
                errorType = ErrorType.Offset.WRONG_VALUE,
                params = mapOf(ParamKey.GOT to "0x%X".format(answer.value)),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateUsefulBitCount(step: Step.UsefulBitCount, answer: Answer): ValidationResult {
        if (answer !is Answer.UsefulBitCountValue) return typeMismatch(step, answer)
        return when {
            answer.value <= 0 -> ValidationResult.incorrect(
                errorType = ErrorType.UsefulBitCount.NON_POSITIVE,
                params = mapOf(ParamKey.GOT to answer.value.toString()),
            )
            answer.value != step.expected -> ValidationResult.incorrect(
                errorType = ErrorType.UsefulBitCount.WRONG_VALUE,
                params = mapOf(ParamKey.GOT to answer.value.toString()),
            )
            else -> ValidationResult.correct()
        }
    }

    private fun validateBinary(step: Step.Binary, answer: Answer): ValidationResult {
        if (answer !is Answer.BinaryValue) return typeMismatch(step, answer)

        // Pedagogical order: alphabet first, then length, then value.
        return when {
            answer.bits.isEmpty() -> ValidationResult.incorrect(ErrorType.Binary.EMPTY)

            answer.bits.any { it != '0' && it != '1' } -> ValidationResult.incorrect(
                errorType = ErrorType.Binary.INVALID_CHARACTER,
                params = mapOf(ParamKey.GOT to answer.bits),
            )

            answer.bits.length < step.length -> ValidationResult.incorrect(
                errorType = ErrorType.Binary.TOO_FEW_BITS,
                params = mapOf(
                    ParamKey.EXPECTED_LENGTH to step.length.toString(),
                    ParamKey.GOT_LENGTH to answer.bits.length.toString(),
                ),
            )

            answer.bits.length > step.length -> ValidationResult.incorrect(
                errorType = ErrorType.Binary.TOO_MANY_BITS,
                params = mapOf(
                    ParamKey.EXPECTED_LENGTH to step.length.toString(),
                    ParamKey.GOT_LENGTH to answer.bits.length.toString(),
                ),
            )

            answer.bits != step.expected -> ValidationResult.incorrect(
                errorType = ErrorType.Binary.WRONG_VALUE,
                params = mapOf(ParamKey.GOT to answer.bits),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateFormat(step: Step.Format, answer: Answer): ValidationResult {
        if (answer !is Answer.FormatChoice) return typeMismatch(step, answer)

        return when {
            answer.value.isEmpty() -> ValidationResult.incorrect(ErrorType.Format.EMPTY)

            // The choices list is public (already shown in the UI) - safe to echo back.
            answer.value !in step.choices -> ValidationResult.incorrect(
                errorType = ErrorType.Format.UNKNOWN_CHOICE,
                params = mapOf(
                    ParamKey.GOT to answer.value,
                    ParamKey.CHOICES to step.choices.joinToString(", "),
                ),
            )

            answer.value != step.expected -> ValidationResult.incorrect(
                errorType = ErrorType.Format.WRONG_CHOICE,
                params = mapOf(ParamKey.GOT to answer.value),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateBitGroups(step: Step.BitGroups, answer: Answer): ValidationResult {
        if (answer !is Answer.BitGroupsValue) return typeMismatch(step, answer)

        if (answer.groups.isEmpty()) {
            return ValidationResult.incorrect(ErrorType.BitGroups.EMPTY)
        }

        if (answer.groups.size != step.expected.size) {
            return ValidationResult.incorrect(
                errorType = ErrorType.BitGroups.WRONG_GROUP_COUNT,
                params = mapOf(
                    ParamKey.EXPECTED_COUNT to step.expected.size.toString(),
                    ParamKey.GOT_COUNT to answer.groups.size.toString(),
                ),
            )
        }

        // Per-group: alphabet first, then length (structural hints - no expected value leak).
        for ((index, group) in answer.groups.withIndex()) {
            val expectedLength = step.expected[index].length
            if (group.any { it != '0' && it != '1' }) {
                return ValidationResult.incorrect(
                    errorType = ErrorType.BitGroups.INVALID_CHARACTER,
                    params = mapOf(ParamKey.POSITION to index.toString(), ParamKey.GOT to group),
                )
            }
            if (group.length != expectedLength) {
                return ValidationResult.incorrect(
                    errorType = ErrorType.BitGroups.WRONG_GROUP_LENGTH,
                    params = mapOf(
                        ParamKey.POSITION to index.toString(),
                        ParamKey.EXPECTED_LENGTH to expectedLength.toString(),
                        ParamKey.GOT_LENGTH to group.length.toString(),
                    ),
                )
            }
        }

        return if (answer.groups != step.expected) {
            ValidationResult.incorrect(
                errorType = ErrorType.BitGroups.WRONG_VALUE,
                params = mapOf(ParamKey.GOT to answer.groups.joinToString(" ")),
            )
        } else {
            ValidationResult.correct()
        }
    }

    private fun validateHexBytes(step: Step.HexBytes, answer: Answer): ValidationResult {
        if (answer !is Answer.HexBytesValue) return typeMismatch(step, answer)

        if (answer.bytes.isEmpty()) {
            return ValidationResult.incorrect(ErrorType.HexBytes.EMPTY)
        }

        // Any byte must fit in an octet: 0..255 unsigned.
        val invalidIndex = answer.bytes.indexOfFirst { it !in 0..0xFF }
        if (invalidIndex >= 0) {
            return ValidationResult.incorrect(
                errorType = ErrorType.HexBytes.BYTE_OUT_OF_RANGE,
                params = mapOf(
                    ParamKey.POSITION to invalidIndex.toString(),
                    ParamKey.GOT to answer.bytes[invalidIndex].toString(),
                ),
            )
        }

        return when {
            answer.bytes.size < step.expected.size -> ValidationResult.incorrect(
                errorType = ErrorType.HexBytes.TOO_FEW_BYTES,
                params = mapOf(
                    ParamKey.EXPECTED_COUNT to step.expected.size.toString(),
                    ParamKey.GOT_COUNT to answer.bytes.size.toString(),
                ),
            )

            answer.bytes.size > step.expected.size -> ValidationResult.incorrect(
                errorType = ErrorType.HexBytes.TOO_MANY_BYTES,
                params = mapOf(
                    ParamKey.EXPECTED_COUNT to step.expected.size.toString(),
                    ParamKey.GOT_COUNT to answer.bytes.size.toString(),
                ),
            )

            answer.bytes != step.expected -> ValidationResult.incorrect(
                errorType = ErrorType.HexBytes.WRONG_VALUE,
                params = mapOf(
                    ParamKey.GOT to answer.bytes.joinToString(" ") { "%02X".format(it) },
                ),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateCodePoint(step: Step.CodePointEntry, answer: Answer): ValidationResult {
        if (answer !is Answer.CodePointValue) return typeMismatch(step, answer)

        return when {
            // Unicode range is public info, not the answer - safe to echo bounds.
            answer.value !in 0..0x10FFFF -> ValidationResult.incorrect(
                errorType = ErrorType.CodePoint.OUT_OF_RANGE,
                params = mapOf(
                    ParamKey.GOT to answer.value.toString(),
                    ParamKey.MIN to "0",
                    ParamKey.MAX to "0x10FFFF",
                ),
            )

            answer.value in 0xD800..0xDFFF -> ValidationResult.incorrect(
                errorType = ErrorType.CodePoint.SURROGATE,
                params = mapOf(ParamKey.GOT to "U+%04X".format(answer.value)),
            )

            answer.value != step.expected -> ValidationResult.incorrect(
                errorType = ErrorType.CodePoint.WRONG_VALUE,
                params = mapOf(ParamKey.GOT to "U+%04X".format(answer.value)),
            )

            else -> ValidationResult.correct()
        }
    }

    private fun validateEndianness(step: Step.Endianness, answer: Answer): ValidationResult {
        if (answer !is Answer.EndiannessChoice) return typeMismatch(step, answer)

        // With only two possible values, revealing "got" implicitly reveals the answer.
        // The errorType alone is the feedback - no params needed.
        return if (answer.value != step.expected) {
            ValidationResult.incorrect(ErrorType.Endianness.WRONG_CHOICE)
        } else {
            ValidationResult.correct()
        }
    }

    private fun typeMismatch(step: Step, answer: Answer): ValidationResult = ValidationResult.incorrect(
        errorType = ErrorType.Answer.TYPE_MISMATCH,
        params = mapOf(
            ParamKey.EXPECTED_TYPE to step.type.id,
            ParamKey.GOT_TYPE to answer::class.simpleName.orEmpty(),
        ),
    )
}
