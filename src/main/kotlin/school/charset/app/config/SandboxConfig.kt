package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.encoding.Codec
import school.charset.app.domain.exercise.generator.ByteArrayGenerator
import school.charset.app.domain.exercise.generator.CodePointGenerator
import school.charset.app.domain.exercise.generator.Utf8Generator
import school.charset.app.domain.sandbox.SandboxBytesParser
import school.charset.app.domain.sandbox.SandboxInputParser
import school.charset.app.domain.sandbox.SandboxService
import school.charset.app.infrastructure.http.sandbox.serde.StepSerializer
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.module.SimpleModule
import kotlin.random.Random

@Configuration
class SandboxConfig {
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
    fun utf8Generator(
        codec: Codec,
        codePointGenerator: CodePointGenerator,
        byteArrayGenerator: ByteArrayGenerator,
    ): Utf8Generator = Utf8Generator(codec, codePointGenerator, byteArrayGenerator)

    @Bean
    fun sandboxService(utf8Generator: Utf8Generator): SandboxService = SandboxService(utf8Generator)

    @Bean
    fun sandboxInputParser(): SandboxInputParser = SandboxInputParser()

    @Bean
    fun sandboxBytesParser(): SandboxBytesParser = SandboxBytesParser()

    @Bean
    fun sandboxJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(StepSerializer())
    }
}
