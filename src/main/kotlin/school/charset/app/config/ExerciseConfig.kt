package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.encoding.Encoding
import school.charset.app.domain.exercise.AnswerValidator
import school.charset.app.domain.exercise.ExerciseAttemptRepository
import school.charset.app.domain.exercise.ExerciseService
import school.charset.app.domain.exercise.generator.AsciiGenerator
import school.charset.app.domain.exercise.generator.ByteArrayGenerator
import school.charset.app.domain.exercise.generator.CodePointGenerator
import school.charset.app.domain.exercise.generator.EncodingExerciseGenerator
import school.charset.app.domain.exercise.generator.ExerciseGenerator
import school.charset.app.domain.exercise.generator.Latin1Generator
import school.charset.app.domain.exercise.generator.Utf16ExerciseGenerator
import school.charset.app.domain.exercise.generator.Utf16Generator
import school.charset.app.domain.exercise.generator.Utf32ExerciseGenerator
import school.charset.app.domain.exercise.generator.Utf32Generator
import school.charset.app.domain.exercise.generator.Utf8Generator
import school.charset.app.domain.exercise.generator.Windows1252Generator
import school.charset.app.domain.progress.ProgressService
import school.charset.app.infrastructure.http.exercise.ExerciseStepDto
import school.charset.app.infrastructure.http.exercise.serde.ExerciseStepDtoSerializer
import school.charset.app.infrastructure.repository.exercise.ExposedExerciseAttemptRepository
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.module.SimpleModule
import kotlin.random.Random
import kotlin.time.Clock

@Configuration
class ExerciseConfig {

    @Bean
    fun codec(): Codec = Codec()

    @Bean
    fun random(): Random = Random.Default

    @Bean
    fun codePointGenerator(random: Random): CodePointGenerator = CodePointGenerator(random)

    @Bean
    fun byteArrayGenerator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
    ): ByteArrayGenerator = ByteArrayGenerator(codec, codePointGenerator)

    @Bean
    fun asciiGenerator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
    ): AsciiGenerator = AsciiGenerator(codec, codePointGenerator, byteArrayGenerator)

    @Bean
    fun latin1Generator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
    ): Latin1Generator = Latin1Generator(codec, codePointGenerator, byteArrayGenerator)

    @Bean
    fun windows1252Generator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
    ): Windows1252Generator = Windows1252Generator(codec, codePointGenerator, byteArrayGenerator)

    @Bean
    fun utf8Generator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
    ): Utf8Generator = Utf8Generator(codec, codePointGenerator, byteArrayGenerator)

    @Bean
    fun utf16Generator(codec: Codec): Utf16Generator = Utf16Generator(codec)

    @Bean
    fun utf32Generator(codec: Codec): Utf32Generator = Utf32Generator(codec)

    @Bean
    fun utf16BeExerciseGenerator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
        utf16Generator: Utf16Generator,
    ): Utf16ExerciseGenerator = Utf16ExerciseGenerator(codec, Encoding.Utf16Be, codePointGenerator, byteArrayGenerator, utf16Generator)

    @Bean
    fun utf16LeExerciseGenerator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
        utf16Generator: Utf16Generator,
    ): Utf16ExerciseGenerator = Utf16ExerciseGenerator(codec, Encoding.Utf16Le, codePointGenerator, byteArrayGenerator, utf16Generator)

    @Bean
    fun utf32BeExerciseGenerator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
        utf32Generator: Utf32Generator,
    ): Utf32ExerciseGenerator = Utf32ExerciseGenerator(codec, Encoding.Utf32Be, codePointGenerator, byteArrayGenerator, utf32Generator)

    @Bean
    fun utf32LeExerciseGenerator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
        utf32Generator: Utf32Generator,
    ): Utf32ExerciseGenerator = Utf32ExerciseGenerator(codec, Encoding.Utf32Le, codePointGenerator, byteArrayGenerator, utf32Generator)

    @Bean
    fun exerciseGenerator(generators: Set<EncodingExerciseGenerator>): ExerciseGenerator = ExerciseGenerator(generators)

    @Bean
    fun answerValidator(): AnswerValidator = AnswerValidator()

    @Bean
    fun exerciseAttemptRepository(clock: Clock): ExerciseAttemptRepository = ExposedExerciseAttemptRepository(clock)

    @Bean
    fun exerciseService(
        exerciseGenerator: ExerciseGenerator,
        attemptRepository: ExerciseAttemptRepository,
        answerValidator: AnswerValidator,
        progressService: ProgressService,
        random: Random,
    ): ExerciseService = ExerciseService(
        exerciseGenerator = exerciseGenerator,
        attemptRepository = attemptRepository,
        answerValidator = answerValidator,
        progressService = progressService,
        random = random,
    )

    @Bean
    fun exerciseJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(ExerciseStepDto::class.java, ExerciseStepDtoSerializer())
    }
}
