package school.charset.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "school.charset.app.config",
        "school.charset.app.infrastructure.http",
    ],
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
