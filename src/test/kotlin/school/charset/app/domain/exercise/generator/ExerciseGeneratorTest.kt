package school.charset.app.domain.exercise.generator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import school.charset.app.domain.encoding.CodePoint
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.Exercise
import school.charset.app.domain.exercise.ExerciseGenerationException
import school.charset.app.domain.exercise.Granularity

class ExerciseGeneratorTest :
    FreeSpec({
        // ExerciseGenerator only dispatches - per-encoding logic is covered in
        // each encoding's own test (e.g. AsciiGeneratorTest).

        fun mockGenerator(encoding: Encoding): EncodingExerciseGenerator {
            val mock = mockk<EncodingExerciseGenerator>()
            every { mock.encoding } returns encoding
            return mock
        }

        "generateEncode" - {
            "dispatches to the matching generator" {
                val asciiGenerator = mockGenerator(Encoding.Ascii)
                val expected = Exercise.Encode(
                    codePoint = CodePoint(0x41),
                    encoding = Encoding.Ascii,
                    level = 1,
                    granularity = Granularity.Verbose,
                    steps = emptyList(),
                )
                every {
                    asciiGenerator.generateEncode(level = 1, Granularity.Verbose)
                } returns expected

                val sut = ExerciseGenerator(setOf(asciiGenerator))
                val result = sut.generateEncode(Encoding.Ascii, level = 1, Granularity.Verbose)

                result shouldBe expected
                verify(exactly = 1) { asciiGenerator.generateEncode(1, Granularity.Verbose) }
            }

            "unsupported encoding throws ExerciseGenerationException" {
                val sut = ExerciseGenerator(setOf(mockGenerator(Encoding.Ascii)))
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generateEncode(Encoding.Utf8, level = 1, Granularity.Verbose)
                }
                exception.encoding shouldBe Encoding.Utf8
                exception.level shouldBe 1
            }
        }

        "generateDecode" - {
            "dispatches to the matching generator" {
                val asciiGenerator = mockGenerator(Encoding.Ascii)
                val expected = Exercise.Decode(
                    bytes = byteArrayOf(0x41),
                    encoding = Encoding.Ascii,
                    level = 1,
                    granularity = Granularity.Verbose,
                    steps = emptyList(),
                )
                every {
                    asciiGenerator.generateDecode(level = 1, Granularity.Verbose)
                } returns expected

                val sut = ExerciseGenerator(setOf(asciiGenerator))
                val result = sut.generateDecode(Encoding.Ascii, level = 1, Granularity.Verbose)

                result shouldBe expected
                verify(exactly = 1) { asciiGenerator.generateDecode(1, Granularity.Verbose) }
            }

            "unsupported encoding throws ExerciseGenerationException" {
                val sut = ExerciseGenerator(setOf(mockGenerator(Encoding.Ascii)))
                val exception = shouldThrow<ExerciseGenerationException> {
                    sut.generateDecode(Encoding.Utf8, level = 1, Granularity.Verbose)
                }
                exception.encoding shouldBe Encoding.Utf8
            }
        }

        "empty generator set - every request throws" {
            val sut = ExerciseGenerator(generators = emptySet())
            shouldThrow<ExerciseGenerationException> {
                sut.generateEncode(Encoding.Ascii, level = 1, Granularity.Verbose)
            }
            shouldThrow<ExerciseGenerationException> {
                sut.generateDecode(Encoding.Ascii, level = 1, Granularity.Verbose)
            }
        }

        "duplicate registration throws at construction" {
            shouldThrow<IllegalArgumentException> {
                ExerciseGenerator(
                    generators = setOf(
                        mockGenerator(Encoding.Ascii),
                        mockGenerator(Encoding.Ascii),
                    ),
                )
            }
        }
    })
