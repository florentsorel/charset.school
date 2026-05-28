package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.exercise.generator.Latin1Generator
import school.charset.app.domain.exercise.generator.Utf16Generator
import school.charset.app.domain.exercise.generator.Utf32Generator
import school.charset.app.domain.exercise.generator.Utf8Generator
import school.charset.app.domain.exercise.generator.Windows1252Generator
import school.charset.app.domain.sandbox.SandboxBytesParser
import school.charset.app.domain.sandbox.SandboxEndianParser
import school.charset.app.domain.sandbox.SandboxInputParser
import school.charset.app.domain.sandbox.SandboxService
import school.charset.app.infrastructure.http.sandbox.serde.StepSerializer
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.module.SimpleModule

@Configuration
class SandboxConfig {

    @Bean
    fun sandboxService(
        utf8Generator: Utf8Generator,
        utf16Generator: Utf16Generator,
        utf32Generator: Utf32Generator,
        windows1252Generator: Windows1252Generator,
        latin1Generator: Latin1Generator,
    ): SandboxService = SandboxService(utf8Generator, utf16Generator, utf32Generator, windows1252Generator, latin1Generator)

    @Bean
    fun sandboxInputParser(): SandboxInputParser = SandboxInputParser()

    @Bean
    fun sandboxBytesParser(): SandboxBytesParser = SandboxBytesParser()

    @Bean
    fun sandboxEndianParser(): SandboxEndianParser = SandboxEndianParser()

    @Bean
    fun sandboxJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(StepSerializer())
    }
}
