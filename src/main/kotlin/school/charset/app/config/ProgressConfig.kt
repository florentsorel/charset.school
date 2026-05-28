package school.charset.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import school.charset.app.domain.progress.ProgressRepository
import school.charset.app.domain.progress.ProgressService
import school.charset.app.infrastructure.http.progress.serde.ModuleProgressSerializer
import school.charset.app.infrastructure.repository.progress.ExposedProgressRepository
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.module.SimpleModule
import kotlin.time.Clock

@Configuration
class ProgressConfig {

    @Bean
    fun progressRepository(clock: Clock): ProgressRepository = ExposedProgressRepository(clock)

    @Bean
    fun progressService(
        progressRepository: ProgressRepository,
        clock: Clock,
    ): ProgressService = ProgressService(progressRepository, clock)

    @Bean
    fun progressJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(ModuleProgressSerializer())
    }
}
