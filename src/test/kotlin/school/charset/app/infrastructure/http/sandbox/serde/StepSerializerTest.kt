package school.charset.app.infrastructure.http.sandbox.serde

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.FormatChoice
import school.charset.app.domain.exercise.Step
import school.charset.app.test.ObjectMapperTestUtils.withSerializer
import tools.jackson.databind.json.JsonMapper

class StepSerializerTest :
    FunSpec({
        val mapper = JsonMapper().withSerializer(StepSerializer())

        fun Step.serializeAndAssert(expectedJson: String) {
            val actual = mapper.writeValueAsString(this)
            mapper.readTree(actual) shouldBe mapper.readTree(expectedJson)
        }

        test("Step.Format -> {type, choices, value}") {
            Step.Format(
                choices = listOf(
                    FormatChoice.ONE_BYTE,
                    FormatChoice.TWO_BYTES,
                    FormatChoice.THREE_BYTES,
                    FormatChoice.FOUR_BYTES,
                ),
                expected = FormatChoice.TWO_BYTES,
            ).serializeAndAssert(
                """
                {
                    "type": "format",
                    "choices": [
                        "format-choice.byte-count.1",
                        "format-choice.byte-count.2",
                        "format-choice.byte-count.3",
                        "format-choice.byte-count.4"
                    ],
                    "value": "format-choice.byte-count.2"
                }
                """,
            )
        }

        test("Step.Binary -> {type, value, length}") {
            Step.Binary(expected = "00011101001", length = 11).serializeAndAssert(
                """
                {
                    "type": "binary",
                    "value": "00011101001",
                    "length": 11
                }
                """,
            )
        }

        test("Step.BitGroups -> {type, groups}") {
            Step.BitGroups(expected = listOf("00011", "101001")).serializeAndAssert(
                """
                {
                    "type": "bit-groups",
                    "groups": ["00011", "101001"]
                }
                """,
            )
        }

        test("Step.HexBytes -> {type, bytes}") {
            Step.HexBytes(expected = listOf(0xF0, 0x9F, 0x8E, 0x89)).serializeAndAssert(
                """
                {
                    "type": "hex-bytes",
                    "bytes": [240, 159, 142, 137]
                }
                """,
            )
        }

        test("Step.CodePointEntry -> {type, value}") {
            Step.CodePointEntry(expected = 0xE9).serializeAndAssert(
                """
                {
                    "type": "code-point",
                    "value": 233
                }
                """,
            )
        }

        test("Step.Endianness (BigEndian) -> {type, value: \"big\"}") {
            Step.Endianness(expected = Encoding.Endian.BigEndian).serializeAndAssert(
                """
                {
                    "type": "endianness",
                    "value": "big"
                }
                """,
            )
        }

        test("Step.Endianness (LittleEndian) -> {type, value: \"little\"}") {
            Step.Endianness(expected = Encoding.Endian.LittleEndian).serializeAndAssert(
                """
                {
                    "type": "endianness",
                    "value": "little"
                }
                """,
            )
        }
    })
