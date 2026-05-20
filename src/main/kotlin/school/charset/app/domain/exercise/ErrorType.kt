package school.charset.app.domain.exercise

// Stable identifiers for validation errors produced by `AnswerValidator`.
object ErrorType {
    object Answer {
        const val TYPE_MISMATCH = "answer.type-mismatch"
    }

    object Binary {
        const val EMPTY = "binary.empty"
        const val INVALID_CHARACTER = "binary.invalid-character"
        const val TOO_FEW_BITS = "binary.too-few-bits"
        const val TOO_MANY_BITS = "binary.too-many-bits"
        const val WRONG_VALUE = "binary.wrong-value"
    }

    object Format {
        const val EMPTY = "format.empty"
        const val UNKNOWN_CHOICE = "format.unknown-choice"
        const val WRONG_CHOICE = "format.wrong-choice"
    }

    object BitGroups {
        const val EMPTY = "bit-groups.empty"
        const val WRONG_GROUP_COUNT = "bit-groups.wrong-group-count"
        const val INVALID_CHARACTER = "bit-groups.invalid-character"
        const val WRONG_GROUP_LENGTH = "bit-groups.wrong-group-length"
        const val WRONG_VALUE = "bit-groups.wrong-value"
    }

    object HexBytes {
        const val EMPTY = "hex-bytes.empty"
        const val BYTE_OUT_OF_RANGE = "hex-bytes.byte-out-of-range"
        const val TOO_FEW_BYTES = "hex-bytes.too-few-bytes"
        const val TOO_MANY_BYTES = "hex-bytes.too-many-bytes"
        const val WRONG_VALUE = "hex-bytes.wrong-value"
    }

    object CodePoint {
        const val OUT_OF_RANGE = "code-point.out-of-range"
        const val SURROGATE = "code-point.surrogate"
        const val WRONG_VALUE = "code-point.wrong-value"
    }

    object Endianness {
        const val WRONG_CHOICE = "endianness.wrong-choice"
    }
}
