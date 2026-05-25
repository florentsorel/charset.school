FROM eclipse-temurin:25-jdk AS builder
WORKDIR /opt/app/sources

COPY . .

RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test


RUN java -Djarmode=tools \
        -jar build/libs/charset.school.jar \
        extract \
        --destination /opt/app/output \
        --layers \
        --launcher

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && chown -R app:app /app
USER app

COPY --chown=app:app --from=builder /opt/app/output/dependencies/ ./
COPY --chown=app:app --from=builder /opt/app/output/spring-boot-loader/ ./
COPY --chown=app:app --from=builder /opt/app/output/snapshot-dependencies/ ./
COPY --chown=app:app --from=builder /opt/app/output/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
