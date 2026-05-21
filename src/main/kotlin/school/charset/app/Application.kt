package school.charset.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "school.charset.app.config",
    ],
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
