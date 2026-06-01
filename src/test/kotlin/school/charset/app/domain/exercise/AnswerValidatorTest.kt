package school.charset.app.domain.exercise

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.Encoding

class AnswerValidatorTest :
    FreeSpec({
        val sut = AnswerValidator()

        "binary" - {
            val step = Step.Binary(expected = "11101001", length = 8)

            "correct value -> ok" {
                sut.validate(step, Answer.BinaryValue("11101001")) shouldBe
                    ValidationResult.correct()
            }

            "empty input -> binary.empty" {
                sut.validate(step, Answer.BinaryValue("")) shouldBe
                    ValidationResult.incorrect(errorType = ErrorType.Binary.EMPTY)
            }

            "non-binary character -> binary.invalid-character" {
                sut.validate(step, Answer.BinaryValue("1110A001")) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Binary.INVALID_CHARACTER,
                        params = mapOf(ParamKey.GOT to "1110A001"),
                    )
            }

            "shorter than expected -> binary.too-few-bits" {
                sut.validate(step, Answer.BinaryValue("1110100")) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Binary.TOO_FEW_BITS,
                        params = mapOf(
                            ParamKey.EXPECTED_LENGTH to "8",
                            ParamKey.GOT_LENGTH to "7",
                        ),
                    )
            }

            "longer than expected -> binary.too-many-bits" {
                sut.validate(step, Answer.BinaryValue("111010010")) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Binary.TOO_MANY_BITS,
                        params = mapOf(
                            ParamKey.EXPECTED_LENGTH to "8",
                            ParamKey.GOT_LENGTH to "9",
                        ),
                    )
            }

            "right length but wrong value -> binary.wrong-value (no expected leak)" {
                sut.validate(step, Answer.BinaryValue("00000000")) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Binary.WRONG_VALUE,
                        params = mapOf(ParamKey.GOT to "00000000"),
                    )
            }

            "alphabet checked before length (mostly invalid chars, wrong length)" {
                val result = sut.validate(step, Answer.BinaryValue("ABCD"))
                result.ok shouldBe false
                result.errorType shouldBe ErrorType.Binary.INVALID_CHARACTER
            }

            "length checked before value (right alphabet, wrong length)" {
                val result = sut.validate(step, Answer.BinaryValue("11111111111"))
                result.ok shouldBe false
                result.errorType shouldBe ErrorType.Binary.TOO_MANY_BITS
            }

            "wrong Answer type -> answer.type-mismatch" {
                sut.validate(step, Answer.HexBytesValue(listOf(0xE9))) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Answer.TYPE_MISMATCH,
                        params = mapOf(
                            ParamKey.EXPECTED_TYPE to "binary",
                            ParamKey.GOT_TYPE to "HexBytesValue",
                        ),
                    )
            }
        }

        "format" - {
            val step = Step.Format(
                choices = listOf("1 byte", "2 bytes", "3 bytes", "4 bytes"),
                expected = "2 bytes",
            )

            "correct choice -> ok" {
                sut.validate(step, Answer.FormatChoice("2 bytes")) shouldBe
                    ValidationResult.correct()
            }

            "empty -> format.empty" {
                sut.validate(step, Answer.FormatChoice("")) shouldBe
                    ValidationResult.incorrect(errorType = ErrorType.Format.EMPTY)
            }

            "choice not in list -> format.unknown-choice" {
                sut.validate(step, Answer.FormatChoice("5 bytes")) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Format.UNKNOWN_CHOICE,
                        params = mapOf(
                            ParamKey.GOT to "5 bytes",
                            ParamKey.CHOICES to "1 byte, 2 bytes, 3 bytes, 4 bytes",
                        ),
                    )
            }

            "wrong choice -> format.wrong-choice (no expected leak)" {
                sut.validate(step, Answer.FormatChoice("3 bytes")) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Format.WRONG_CHOICE,
                        params = mapOf(ParamKey.GOT to "3 bytes"),
                    )
            }

            "wrong Answer type -> answer.type-mismatch" {
                val result = sut.validate(step, Answer.BinaryValue("0"))
                result.errorType shouldBe ErrorType.Answer.TYPE_MISMATCH
                result.params[ParamKey.EXPECTED_TYPE] shouldBe "format"
            }
        }

        "bit-groups" - {
            val step = Step.BitGroups(expected = listOf("00011", "101001"))

            "correct value -> ok" {
                sut.validate(step, Answer.BitGroupsValue(listOf("00011", "101001"))) shouldBe
                    ValidationResult.correct()
            }

            "empty -> bit-groups.empty" {
                sut.validate(step, Answer.BitGroupsValue(emptyList())) shouldBe
                    ValidationResult.incorrect(errorType = ErrorType.BitGroups.EMPTY)
            }

            "wrong group count (too few) -> bit-groups.wrong-group-count" {
                sut.validate(step, Answer.BitGroupsValue(listOf("00011"))) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.BitGroups.WRONG_GROUP_COUNT,
                        params = mapOf(
                            ParamKey.EXPECTED_COUNT to "2",
                            ParamKey.GOT_COUNT to "1",
                        ),
                    )
            }

            "wrong group count (too many) -> bit-groups.wrong-group-count" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("00011", "101001", "111")),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.BitGroups.WRONG_GROUP_COUNT,
                        params = mapOf(
                            ParamKey.EXPECTED_COUNT to "2",
                            ParamKey.GOT_COUNT to "3",
                        ),
                    )
            }

            "invalid char in second group -> bit-groups.invalid-character" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("00011", "10A001")),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.BitGroups.INVALID_CHARACTER,
                        params = mapOf(ParamKey.POSITION to "1", ParamKey.GOT to "10A001"),
                    )
            }

            "wrong length on first group -> bit-groups.wrong-group-length" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("0001", "101001")),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.BitGroups.WRONG_GROUP_LENGTH,
                        params = mapOf(
                            ParamKey.POSITION to "0",
                            ParamKey.EXPECTED_LENGTH to "5",
                            ParamKey.GOT_LENGTH to "4",
                        ),
                    )
            }

            "right structure but wrong bits -> bit-groups.wrong-value (no expected leak)" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("11111", "000000")),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.BitGroups.WRONG_VALUE,
                        params = mapOf(ParamKey.GOT to "11111 000000"),
                    )
            }

            "count checked before per-group checks" {
                val result = sut.validate(step, Answer.BitGroupsValue(listOf("ABCDE")))
                result.errorType shouldBe ErrorType.BitGroups.WRONG_GROUP_COUNT
            }

            "alphabet checked before length on same group" {
                val result = sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("00011", "AB")),
                )
                result.errorType shouldBe ErrorType.BitGroups.INVALID_CHARACTER
            }
        }

        "hex-bytes" - {
            val step = Step.HexBytes(expected = listOf(0xC3, 0xA9))

            "correct value -> ok" {
                sut.validate(step, Answer.HexBytesValue(listOf(0xC3, 0xA9))) shouldBe
                    ValidationResult.correct()
            }

            "empty -> hex-bytes.empty" {
                sut.validate(step, Answer.HexBytesValue(emptyList())) shouldBe
                    ValidationResult.incorrect(errorType = ErrorType.HexBytes.EMPTY)
            }

            "byte > 255 -> hex-bytes.byte-out-of-range" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0x100)),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.HexBytes.BYTE_OUT_OF_RANGE,
                        params = mapOf(ParamKey.POSITION to "1", ParamKey.GOT to "256"),
                    )
            }

            "byte < 0 -> hex-bytes.byte-out-of-range" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(-1, 0xA9)),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.HexBytes.BYTE_OUT_OF_RANGE,
                        params = mapOf(ParamKey.POSITION to "0", ParamKey.GOT to "-1"),
                    )
            }

            "too few bytes -> hex-bytes.too-few-bytes" {
                sut.validate(step, Answer.HexBytesValue(listOf(0xC3))) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.HexBytes.TOO_FEW_BYTES,
                        params = mapOf(
                            ParamKey.EXPECTED_COUNT to "2",
                            ParamKey.GOT_COUNT to "1",
                        ),
                    )
            }

            "too many bytes -> hex-bytes.too-many-bytes" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0xA9, 0x00)),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.HexBytes.TOO_MANY_BYTES,
                        params = mapOf(
                            ParamKey.EXPECTED_COUNT to "2",
                            ParamKey.GOT_COUNT to "3",
                        ),
                    )
            }

            "right count but wrong bytes -> hex-bytes.wrong-value (no expected leak)" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0xAA)),
                ) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.HexBytes.WRONG_VALUE,
                        params = mapOf(ParamKey.GOT to "C3 AA"),
                    )
            }

            "range checked before count" {
                val result = sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0xA9, 300)),
                )
                result.errorType shouldBe ErrorType.HexBytes.BYTE_OUT_OF_RANGE
            }
        }

        "code-point" - {
            val step = Step.CodePointEntry(expected = 0x00E9)

            "correct value -> ok" {
                sut.validate(step, Answer.CodePointValue(0x00E9)) shouldBe
                    ValidationResult.correct()
            }

            "negative -> code-point.out-of-range" {
                sut.validate(step, Answer.CodePointValue(-1)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.CodePoint.OUT_OF_RANGE,
                        params = mapOf(
                            ParamKey.GOT to "-1",
                            ParamKey.MIN to "0",
                            ParamKey.MAX to "0x10FFFF",
                        ),
                    )
            }

            "above U+10FFFF -> code-point.out-of-range" {
                sut.validate(step, Answer.CodePointValue(0x110000)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.CodePoint.OUT_OF_RANGE,
                        params = mapOf(
                            ParamKey.GOT to "1114112",
                            ParamKey.MIN to "0",
                            ParamKey.MAX to "0x10FFFF",
                        ),
                    )
            }

            "high surrogate -> code-point.surrogate" {
                sut.validate(step, Answer.CodePointValue(0xD800)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.CodePoint.SURROGATE,
                        params = mapOf(ParamKey.GOT to "U+D800"),
                    )
            }

            "low surrogate -> code-point.surrogate" {
                sut.validate(step, Answer.CodePointValue(0xDFFF)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.CodePoint.SURROGATE,
                        params = mapOf(ParamKey.GOT to "U+DFFF"),
                    )
            }

            "valid but wrong -> code-point.wrong-value (no expected leak)" {
                sut.validate(step, Answer.CodePointValue(0x00EA)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.CodePoint.WRONG_VALUE,
                        params = mapOf(ParamKey.GOT to "U+00EA"),
                    )
            }

            "range checked before surrogate" {
                val result = sut.validate(step, Answer.CodePointValue(0x110000))
                result.errorType shouldBe ErrorType.CodePoint.OUT_OF_RANGE
            }
        }

        "endianness" - {
            val step = Step.Endianness(expected = Encoding.Endian.BigEndian)

            "correct BigEndian -> ok" {
                sut.validate(step, Answer.EndiannessChoice(Encoding.Endian.BigEndian)) shouldBe
                    ValidationResult.correct()
            }

            "correct LittleEndian (LE step) -> ok" {
                val leStep = Step.Endianness(expected = Encoding.Endian.LittleEndian)
                sut.validate(leStep, Answer.EndiannessChoice(Encoding.Endian.LittleEndian)) shouldBe
                    ValidationResult.correct()
            }

            "wrong choice -> endianness.wrong-choice (no got/expected leak)" {
                sut.validate(
                    step,
                    Answer.EndiannessChoice(Encoding.Endian.LittleEndian),
                ) shouldBe
                    ValidationResult.incorrect(errorType = ErrorType.Endianness.WRONG_CHOICE)
            }

            "wrong Answer type -> answer.type-mismatch" {
                val result = sut.validate(step, Answer.BinaryValue("0"))
                result.errorType shouldBe ErrorType.Answer.TYPE_MISMATCH
                result.params[ParamKey.EXPECTED_TYPE] shouldBe "endianness"
            }
        }

        "useful-bit-count" - {
            val step = Step.UsefulBitCount(expected = 11)

            "correct value -> ok" {
                sut.validate(step, Answer.UsefulBitCountValue(11)) shouldBe
                    ValidationResult.correct()
            }

            "zero -> useful-bit-count.non-positive" {
                sut.validate(step, Answer.UsefulBitCountValue(0)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.UsefulBitCount.NON_POSITIVE,
                        params = mapOf(ParamKey.GOT to "0"),
                    )
            }

            "negative -> useful-bit-count.non-positive" {
                sut.validate(step, Answer.UsefulBitCountValue(-3)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.UsefulBitCount.NON_POSITIVE,
                        params = mapOf(ParamKey.GOT to "-3"),
                    )
            }

            "wrong positive value -> useful-bit-count.wrong-value" {
                sut.validate(step, Answer.UsefulBitCountValue(16)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.UsefulBitCount.WRONG_VALUE,
                        params = mapOf(ParamKey.GOT to "16"),
                    )
            }

            "wrong Answer type -> answer.type-mismatch" {
                val result = sut.validate(step, Answer.BinaryValue("11"))
                result.errorType shouldBe ErrorType.Answer.TYPE_MISMATCH
                result.params[ParamKey.EXPECTED_TYPE] shouldBe "useful-bit-count"
            }
        }

        "offset" - {
            val step = Step.Offset(expected = 0xF389)

            "correct value -> ok" {
                sut.validate(step, Answer.OffsetValue(0xF389)) shouldBe
                    ValidationResult.correct()
            }

            "negative -> offset.out-of-range" {
                sut.validate(step, Answer.OffsetValue(-1)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Offset.OUT_OF_RANGE,
                        params = mapOf(
                            ParamKey.GOT to "-1",
                            ParamKey.MIN to "0",
                            ParamKey.MAX to "0xFFFFF",
                        ),
                    )
            }

            "above 0xFFFFF -> offset.out-of-range" {
                sut.validate(step, Answer.OffsetValue(0x100000)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Offset.OUT_OF_RANGE,
                        params = mapOf(
                            ParamKey.GOT to "1048576",
                            ParamKey.MIN to "0",
                            ParamKey.MAX to "0xFFFFF",
                        ),
                    )
            }

            "valid but wrong -> offset.wrong-value (no expected leak)" {
                sut.validate(step, Answer.OffsetValue(0xF38A)) shouldBe
                    ValidationResult.incorrect(
                        errorType = ErrorType.Offset.WRONG_VALUE,
                        params = mapOf(ParamKey.GOT to "0xF38A"),
                    )
            }

            "wrong Answer type -> answer.type-mismatch" {
                val result = sut.validate(step, Answer.BinaryValue("11"))
                result.errorType shouldBe ErrorType.Answer.TYPE_MISMATCH
                result.params[ParamKey.EXPECTED_TYPE] shouldBe "offset"
            }
        }
    })
