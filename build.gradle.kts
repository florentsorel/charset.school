import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Instant

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        jvmTarget.set(JvmTarget.JVM_25)
    }
    jvmToolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

repositories {
    mavenCentral()
}

sourceSets {
    val main by getting
    val intTest by creating {
        compileClasspath += main.output
        runtimeClasspath += main.output
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.session:spring-session-jdbc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    implementation(platform("org.jetbrains.exposed:exposed-bom:1.3.0"))
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime")
    implementation("org.postgresql:postgresql")

    // Unit tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("io.kotest:kotest-bom:6.1.11"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk:1.14.9")

    "intTestImplementation"("org.testcontainers:testcontainers-junit-jupiter")
    "intTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "intTestImplementation"("org.testcontainers:testcontainers-postgresql")
}

configurations {
    getByName("intTestImplementation").extendsFrom(configurations.testImplementation.get())
    getByName("intTestRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val intTestTask = tasks.register<Test>("intTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath

    shouldRunAfter("test")
}

tasks.check { dependsOn(intTestTask) }

tasks.register("makeMigration") {
    description = "Creates a new Flyway migration file with a timestamp version."
    group = "flyway"

    doLast {
        val name = (project.findProperty("migrationName") as String?)
            ?: error("Usage: ./gradlew makeMigration -PmigrationName=create_some_table")
        val safeName = name.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val timestamp = Instant.now().epochSecond
        val file = file("src/main/resources/db/migration/V${timestamp}__$safeName.sql")
        file.parentFile.mkdirs()
        file.createNewFile()
        println("Created ${file.relativeTo(projectDir)}")
    }
}

tasks.bootJar {
    archiveFileName.set("charset.school.jar")
}
