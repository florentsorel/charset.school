package school.charset.app.domain.exercise

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.Encoding

class StepTest :
    FreeSpec({
        "Format" - {
            "valid step builds" {
                val step = Step.Format(choices = listOf("a", "b"), expected = "a")
                step.expected shouldBe "a"
            }

            "empty choices throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Format(choices = emptyList(), expected = "x")
                }
            }

            "expected not in choices throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Format(choices = listOf("a", "b"), expected = "c")
                }
            }
        }

        "Binary" - {
            "valid step builds" {
                Step.Binary(expected = "11101001", length = 8).length shouldBe 8
            }

            "zero length throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Binary(expected = "", length = 0)
                }
            }

            "negative length throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Binary(expected = "1", length = -1)
                }
            }

            "expected length mismatching length throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Binary(expected = "1010", length = 8)
                }
            }

            "expected with non-binary character throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Binary(expected = "1110A001", length = 8)
                }
            }
        }

        "BitGroups" - {
            "valid step builds" {
                Step.BitGroups(expected = listOf("00011", "101001")).expected.size shouldBe 2
            }

            "empty list throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.BitGroups(expected = emptyList())
                }
            }

            "empty group throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.BitGroups(expected = listOf("00011", ""))
                }
            }

            "non-binary character in any group throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.BitGroups(expected = listOf("00011", "10A001"))
                }
            }
        }

        "HexBytes" - {
            "valid step builds" {
                Step.HexBytes(expected = listOf(0xC3, 0xA9)).expected.size shouldBe 2
            }

            "empty list throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.HexBytes(expected = emptyList())
                }
            }

            "byte > 255 throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.HexBytes(expected = listOf(0xC3, 0x100))
                }
            }

            "byte < 0 throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.HexBytes(expected = listOf(-1, 0xA9))
                }
            }
        }

        "CodePointEntry" - {
            "valid code point builds" {
                Step.CodePointEntry(expected = 0x00E9).expected shouldBe 0xE9
            }

            "boundary U+0000 builds" {
                Step.CodePointEntry(expected = 0x0000).expected shouldBe 0
            }

            "boundary U+10FFFF builds" {
                Step.CodePointEntry(expected = 0x10FFFF).expected shouldBe 0x10FFFF
            }

            "negative throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.CodePointEntry(expected = -1)
                }
            }

            "above U+10FFFF throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.CodePointEntry(expected = 0x110000)
                }
            }

            "surrogate U+D800 throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.CodePointEntry(expected = 0xD800)
                }
            }

            "surrogate U+DFFF throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.CodePointEntry(expected = 0xDFFF)
                }
            }
        }

        "Endianness" - {
            "BigEndian builds" {
                Step.Endianness(expected = Encoding.Endian.BigEndian).expected shouldBe
                    Encoding.Endian.BigEndian
            }

            "LittleEndian builds" {
                Step.Endianness(expected = Encoding.Endian.LittleEndian).expected shouldBe
                    Encoding.Endian.LittleEndian
            }
        }

        "UsefulBitCount" - {
            "valid count builds" {
                Step.UsefulBitCount(expected = 11).expected shouldBe 11
            }

            "lower bound 1 builds" {
                Step.UsefulBitCount(expected = 1).expected shouldBe 1
            }

            "upper bound 32 builds" {
                Step.UsefulBitCount(expected = 32).expected shouldBe 32
            }

            "zero throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.UsefulBitCount(expected = 0)
                }
            }

            "negative throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.UsefulBitCount(expected = -1)
                }
            }

            "above 32 throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.UsefulBitCount(expected = 33)
                }
            }
        }

        "Offset" - {
            "valid value builds" {
                Step.Offset(expected = 0xF389).expected shouldBe 0xF389
            }

            "lower bound 0 builds" {
                Step.Offset(expected = 0).expected shouldBe 0
            }

            "upper bound 0xFFFFF builds" {
                Step.Offset(expected = 0xFFFFF).expected shouldBe 0xFFFFF
            }

            "negative throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Offset(expected = -1)
                }
            }

            "above 0xFFFFF throws" {
                shouldThrow<IllegalArgumentException> {
                    Step.Offset(expected = 0x100000)
                }
            }
        }
    })
