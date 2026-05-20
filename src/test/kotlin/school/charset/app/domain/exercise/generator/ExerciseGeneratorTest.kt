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
        // ExerciseGenerator only dispatches — per-encoding logic is covered in
        // each encoding's own test (e.g. AsciiGeneratorTest).

        fun mockGenerator(encoding: Encoding): EncodingExerciseGenerator {
            val mock = mockk<EncodingExerciseGenerator>()
            every { mock.encoding } returns encoding
            return mock
        }

        "dispatches to the generator matching the requested encoding" {
            val asciiGenerator = mockGenerator(Encoding.Ascii)
            val expected = Exercise(
                codePoint = CodePoint(0x41),
                encoding = Encoding.Ascii,
                level = 1,
                granularity = Granularity.Verbose,
                steps = emptyList(),
            )
            every { asciiGenerator.generate(level = 1, Granularity.Verbose) } returns expected

            val sut = ExerciseGenerator(setOf(asciiGenerator))
            val result = sut.generate(Encoding.Ascii, level = 1, Granularity.Verbose)

            result shouldBe expected
            verify(exactly = 1) { asciiGenerator.generate(1, Granularity.Verbose) }
        }

        "unsupported encoding throws ExerciseGenerationException" {
            val sut = ExerciseGenerator(setOf(mockGenerator(Encoding.Ascii)))
            val exception = shouldThrow<ExerciseGenerationException> {
                sut.generate(Encoding.Utf8, level = 1, Granularity.Verbose)
            }
            exception.encoding shouldBe Encoding.Utf8
            exception.level shouldBe 1
        }

        "empty generator set is allowed but every request throws" {
            val sut = ExerciseGenerator(generators = emptySet())
            shouldThrow<ExerciseGenerationException> {
                sut.generate(Encoding.Ascii, level = 1, Granularity.Verbose)
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
