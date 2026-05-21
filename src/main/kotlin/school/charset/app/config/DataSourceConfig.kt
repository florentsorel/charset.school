package school.charset.app.config

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@ConfigurationProperties(prefix = "app.datasource")
data class DataSourceProperties(
    val driverClassName: String,
    val url: String,
    val username: String,
    val password: String,
)

@Configuration
@EnableConfigurationProperties(DataSourceProperties::class)
class DataSourceConfig {
    @Bean
    fun dataSource(properties: DataSourceProperties): DataSource = DataSourceBuilder.create()
        .driverClassName(properties.driverClassName)
        .url(properties.url)
        .username(properties.username)
        .password(properties.password)
        .build()

    @Bean
    fun database(dataSource: DataSource): Database = Database.connect(
        dataSource,
        databaseConfig = DatabaseConfig { useNestedTransactions = true },
    )
}
