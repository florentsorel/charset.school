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
                    ValidationResult(ok = true)
            }

            "empty input -> binary.empty" {
                sut.validate(step, Answer.BinaryValue("")) shouldBe
                    ValidationResult(ok = false, errorType = "binary.empty")
            }

            "non-binary character -> binary.invalid-character" {
                sut.validate(step, Answer.BinaryValue("1110A001")) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "binary.invalid-character",
                        params = mapOf("got" to "1110A001"),
                    )
            }

            "shorter than expected -> binary.too-few-bits" {
                sut.validate(step, Answer.BinaryValue("1110100")) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "binary.too-few-bits",
                        params = mapOf("expected-length" to "8", "got-length" to "7"),
                    )
            }

            "longer than expected -> binary.too-many-bits" {
                sut.validate(step, Answer.BinaryValue("111010010")) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "binary.too-many-bits",
                        params = mapOf("expected-length" to "8", "got-length" to "9"),
                    )
            }

            "right length but wrong value -> binary.wrong-value (no expected leak)" {
                sut.validate(step, Answer.BinaryValue("00000000")) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "binary.wrong-value",
                        params = mapOf("got" to "00000000"),
                    )
            }

            "alphabet checked before length (mostly invalid chars, wrong length)" {
                val result = sut.validate(step, Answer.BinaryValue("ABCD"))
                result.ok shouldBe false
                result.errorType shouldBe "binary.invalid-character"
            }

            "length checked before value (right alphabet, wrong length)" {
                val result = sut.validate(step, Answer.BinaryValue("11111111111"))
                result.ok shouldBe false
                result.errorType shouldBe "binary.too-many-bits"
            }

            "wrong Answer type -> answer.type-mismatch" {
                sut.validate(step, Answer.HexBytesValue(listOf(0xE9))) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "answer.type-mismatch",
                        params = mapOf("expected-type" to "binary", "got-type" to "HexBytesValue"),
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
                    ValidationResult(ok = true)
            }

            "empty -> format.empty" {
                sut.validate(step, Answer.FormatChoice("")) shouldBe
                    ValidationResult(ok = false, errorType = "format.empty")
            }

            "choice not in list -> format.unknown-choice" {
                sut.validate(step, Answer.FormatChoice("5 bytes")) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "format.unknown-choice",
                        params = mapOf(
                            "got" to "5 bytes",
                            "choices" to "1 byte, 2 bytes, 3 bytes, 4 bytes",
                        ),
                    )
            }

            "wrong choice -> format.wrong-choice (no expected leak)" {
                sut.validate(step, Answer.FormatChoice("3 bytes")) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "format.wrong-choice",
                        params = mapOf("got" to "3 bytes"),
                    )
            }

            "wrong Answer type -> answer.type-mismatch" {
                val result = sut.validate(step, Answer.BinaryValue("0"))
                result.errorType shouldBe "answer.type-mismatch"
                result.params["expected-type"] shouldBe "format"
            }
        }

        "bit-groups" - {
            val step = Step.BitGroups(expected = listOf("00011", "101001"))

            "correct value -> ok" {
                sut.validate(step, Answer.BitGroupsValue(listOf("00011", "101001"))) shouldBe
                    ValidationResult(ok = true)
            }

            "empty -> bit-groups.empty" {
                sut.validate(step, Answer.BitGroupsValue(emptyList())) shouldBe
                    ValidationResult(ok = false, errorType = "bit-groups.empty")
            }

            "wrong group count (too few) -> bit-groups.wrong-group-count" {
                sut.validate(step, Answer.BitGroupsValue(listOf("00011"))) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "bit-groups.wrong-group-count",
                        params = mapOf("expected-count" to "2", "got-count" to "1"),
                    )
            }

            "wrong group count (too many) -> bit-groups.wrong-group-count" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("00011", "101001", "111")),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "bit-groups.wrong-group-count",
                        params = mapOf("expected-count" to "2", "got-count" to "3"),
                    )
            }

            "invalid char in second group -> bit-groups.invalid-character" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("00011", "10A001")),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "bit-groups.invalid-character",
                        params = mapOf("position" to "1", "got" to "10A001"),
                    )
            }

            "wrong length on first group -> bit-groups.wrong-group-length" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("0001", "101001")),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "bit-groups.wrong-group-length",
                        params = mapOf(
                            "position" to "0",
                            "expected-length" to "5",
                            "got-length" to "4",
                        ),
                    )
            }

            "right structure but wrong bits -> bit-groups.wrong-value (no expected leak)" {
                sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("11111", "000000")),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "bit-groups.wrong-value",
                        params = mapOf("got" to "11111 000000"),
                    )
            }

            "count checked before per-group checks" {
                // 1 group with invalid char — but count is also wrong (1 vs 2).
                // wrong-group-count wins because structure is checked first.
                val result = sut.validate(step, Answer.BitGroupsValue(listOf("ABCDE")))
                result.errorType shouldBe "bit-groups.wrong-group-count"
            }

            "alphabet checked before length on same group" {
                // 2 groups, second has both invalid char AND wrong length.
                val result = sut.validate(
                    step,
                    Answer.BitGroupsValue(listOf("00011", "AB")),
                )
                result.errorType shouldBe "bit-groups.invalid-character"
            }
        }

        "hex-bytes" - {
            val step = Step.HexBytes(expected = listOf(0xC3, 0xA9))

            "correct value -> ok" {
                sut.validate(step, Answer.HexBytesValue(listOf(0xC3, 0xA9))) shouldBe
                    ValidationResult(ok = true)
            }

            "empty -> hex-bytes.empty" {
                sut.validate(step, Answer.HexBytesValue(emptyList())) shouldBe
                    ValidationResult(ok = false, errorType = "hex-bytes.empty")
            }

            "byte > 255 -> hex-bytes.byte-out-of-range" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0x100)),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "hex-bytes.byte-out-of-range",
                        params = mapOf("position" to "1", "got" to "256"),
                    )
            }

            "byte < 0 -> hex-bytes.byte-out-of-range" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(-1, 0xA9)),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "hex-bytes.byte-out-of-range",
                        params = mapOf("position" to "0", "got" to "-1"),
                    )
            }

            "too few bytes -> hex-bytes.too-few-bytes" {
                sut.validate(step, Answer.HexBytesValue(listOf(0xC3))) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "hex-bytes.too-few-bytes",
                        params = mapOf("expected-count" to "2", "got-count" to "1"),
                    )
            }

            "too many bytes -> hex-bytes.too-many-bytes" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0xA9, 0x00)),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "hex-bytes.too-many-bytes",
                        params = mapOf("expected-count" to "2", "got-count" to "3"),
                    )
            }

            "right count but wrong bytes -> hex-bytes.wrong-value (no expected leak)" {
                sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0xAA)),
                ) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "hex-bytes.wrong-value",
                        params = mapOf("got" to "C3 AA"),
                    )
            }

            "range checked before count" {
                // 3 bytes (count wrong: 3 vs 2) AND 3rd byte out of range (300 > 255).
                // range error wins because malformed data is more fundamental.
                val result = sut.validate(
                    step,
                    Answer.HexBytesValue(listOf(0xC3, 0xA9, 300)),
                )
                result.errorType shouldBe "hex-bytes.byte-out-of-range"
            }
        }

        "code-point" - {
            val step = Step.CodePointEntry(expected = 0x00E9)

            "correct value -> ok" {
                sut.validate(step, Answer.CodePointValue(0x00E9)) shouldBe
                    ValidationResult(ok = true)
            }

            "negative -> code-point.out-of-range" {
                sut.validate(step, Answer.CodePointValue(-1)) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "code-point.out-of-range",
                        params = mapOf("got" to "-1", "min" to "0", "max" to "0x10FFFF"),
                    )
            }

            "above U+10FFFF -> code-point.out-of-range" {
                sut.validate(step, Answer.CodePointValue(0x110000)) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "code-point.out-of-range",
                        params = mapOf(
                            "got" to "1114112",
                            "min" to "0",
                            "max" to "0x10FFFF",
                        ),
                    )
            }

            "high surrogate -> code-point.surrogate" {
                sut.validate(step, Answer.CodePointValue(0xD800)) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "code-point.surrogate",
                        params = mapOf("got" to "U+D800"),
                    )
            }

            "low surrogate -> code-point.surrogate" {
                sut.validate(step, Answer.CodePointValue(0xDFFF)) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "code-point.surrogate",
                        params = mapOf("got" to "U+DFFF"),
                    )
            }

            "valid but wrong -> code-point.wrong-value (no expected leak)" {
                sut.validate(step, Answer.CodePointValue(0x00EA)) shouldBe
                    ValidationResult(
                        ok = false,
                        errorType = "code-point.wrong-value",
                        params = mapOf("got" to "U+00EA"),
                    )
            }

            "range checked before surrogate" {
                // U+110000 is also outside range; range error wins.
                val result = sut.validate(step, Answer.CodePointValue(0x110000))
                result.errorType shouldBe "code-point.out-of-range"
            }
        }

        "endianness" - {
            val step = Step.Endianness(expected = Encoding.Endian.BigEndian)

            "correct BigEndian -> ok" {
                sut.validate(step, Answer.EndiannessChoice(Encoding.Endian.BigEndian)) shouldBe
                    ValidationResult(ok = true)
            }

            "correct LittleEndian (LE step) -> ok" {
                val leStep = Step.Endianness(expected = Encoding.Endian.LittleEndian)
                sut.validate(leStep, Answer.EndiannessChoice(Encoding.Endian.LittleEndian)) shouldBe
                    ValidationResult(ok = true)
            }

            "wrong choice -> endianness.wrong-choice (no got/expected leak)" {
                // With 2 choices, even echoing "got" implicitly reveals the answer.
                sut.validate(
                    step,
                    Answer.EndiannessChoice(Encoding.Endian.LittleEndian),
                ) shouldBe
                    ValidationResult(ok = false, errorType = "endianness.wrong-choice")
            }

            "wrong Answer type -> answer.type-mismatch" {
                val result = sut.validate(step, Answer.BinaryValue("0"))
                result.errorType shouldBe "answer.type-mismatch"
                result.params["expected-type"] shouldBe "endianness"
            }
        }
    })
