# CLAUDE.md — Charset Playground

Projet d'exercices interactifs pour apprendre **l'encodage et le décodage** des caractères :
ASCII, Latin-1, Windows-1252, UTF-8, UTF-16, UTF-32, endianness, BOM.

L'utilisateur s'inscrit/se connecte, choisit un module et un niveau, et fait les conversions
à la main, étape par étape, avec validation immédiate et explications pédagogiques en cas
d'erreur. Progression et statistiques persistées en base par utilisateur. Pas de QCM.

---

## Stack

### Backend
- **Kotlin 2.x** + **Spring Boot 4.0** (Spring Framework 7)
- **Spring Web** (REST controllers)
- **Spring Security** (auth session-based, cookie HttpOnly, bcrypt)
- **Spring Session JDBC** (sessions persistées en Postgres)
- **Exposed** (DSL fluide) avec **exposed-kotlin-datetime** pour le mapping
  `kotlinx.datetime.*` ↔ colonnes
- **Postgres 18** + **Flyway** pour les migrations (SQL pur, autoconfig Spring Boot)
- **Bean Validation** (Hibernate Validator) pour la validation des inputs côté HTTP
- **kotlinx-datetime** pour les types temporels en domaine ; conversions aux frontières
  via un module Jackson custom (sérialisation API) et le mapping Exposed (DB)
- **Jackson 3** + `jackson-module-kotlin` (sérialisation JSON sans pollution du domaine)
- **Testcontainers** (Postgres) avec `@ServiceConnection` Spring Boot
- **Kotest** (FunSpec) pour les tests
- **MockK** pour les mocks Kotlin idiomatiques
- **Gradle 9** Kotlin DSL

### Frontend
- **TanStack Start v1** (SSR + Vite + TanStack Router intégré)
- **React 19** + **TypeScript** (strict)
- **TanStack Query** (fetch + cache des appels API vers Spring Boot)
- **TanStack Form** (gestion de formulaires type-safe)
- **Tailwind CSS v4** (`@theme` dans CSS, plugin Vite officiel)
- **shadcn/ui** (composants copiés dans `src/components/ui/`)
- **zod** pour la validation des schémas (formulaires, parsing des réponses API)
- **i18next** + **react-i18next** (FR + EN, FR par défaut)
- **lucide-react** pour les icônes

### Reverse proxy / déploiement
- **Caddy** comme reverse proxy unique (HTTPS auto via Let's Encrypt, sert le bundle SSR
  TanStack Start, proxy `/api/*` vers Spring Boot)
- Spring Boot tourne en JAR exécutable derrière Caddy

---

## Architecture — domain / infrastructure

Pattern **ports & adapters léger** (pas de DDD strict, pas de CQRS).

### Principes

1. **Le domaine est pur Kotlin.** Pas de Spring, pas de Jackson, pas d'Exposed, pas
   d'annotation framework. Uniquement le stdlib Kotlin + kotlinx-datetime.
2. **Le domaine ne définit que ses contrats.** Les interfaces (ports) sont dans `domain/port/`,
   les implémentations concrètes dans `infrastructure/`.
3. **Le wiring est explicite via `@Bean`.** Aucun composant scan sur ton code : pas de
   `@Service`, pas de `@Repository`, pas de `@Component` sur les classes domain ou infra
   non-HTTP. Les beans sont déclarés dans des classes `@Configuration` dédiées.
4. **Annotations Spring uniquement pour ce qui est intrinsèquement Spring.** Controllers
   (`@RestController`), filters Spring Security, classes `@Configuration`. Le reste reste
   en classes Kotlin pures.

### Pourquoi cette approche

- Tests unitaires sur le domaine sans booter Spring (rapide, MockK direct sur les ports)
- Wiring auditable d'un coup d'œil dans `config/`
- Substitution triviale en test via `@TestConfiguration`
- Tests d'intégration ciblés qui ne tirent qu'une config

### Stratégie d'enregistrement des composants

| Composant | Façon d'enregistrer | Pourquoi |
|---|---|---|
| `domain/service/*` | `@Bean` dans `DomainConfig` | Domaine pur |
| `domain/port/*` | rien (interface) | C'est un contrat |
| `infrastructure/persistence/*` | `@Bean` dans `PersistenceConfig` | Pas d'annotation framework leakée |
| `infrastructure/http/*Controller` | `@RestController` direct | Intrinsèquement Spring |
| `infrastructure/security/*` | `@Bean` dans `SecurityConfig` | Centralisation du wiring sécurité |
| `infrastructure/time/SystemClock` | `@Bean` dans `InfrastructureConfig` | Remplaçable en test |
| `infrastructure/http/serialization/*` | `@Bean` Module Jackson dans `JacksonConfig` | Centralisé |
| `config/*Config` | `@Configuration` | C'est leur rôle |
| `DataSource` | Autoconfig Spring Boot via `application.yml` | Pas de raison de réinventer |
| Flyway | Autoconfig Spring Boot | Idem |
| Spring Session JDBC | `@EnableJdbcHttpSession` sur `SecurityConfig` | Annotation Spring de config |

### Restriction du component scan

Spring Boot fait du component scan par défaut sur le package racine de
`@SpringBootApplication`. Pour garantir qu'aucune annotation Spring (`@Service`,
`@Repository`, `@Component`) ne soit accidentellement scannée dans `domain/` ou
`infrastructure/persistence/`, on restreint explicitement le scan à `config/` et
`infrastructure/http/` :

```kotlin
// CharsetApplication.kt
@SpringBootApplication(scanBasePackages = [
    "school.charset.app.config",
    "school.charset.app.infrastructure.http",
])
class CharsetApplication

fun main(args: Array<String>) {
    runApplication<CharsetApplication>(*args)
}
```

Conséquences :

- Si quelqu'un colle un `@Service` sur une classe dans `domain/`, **rien ne se passe** :
  le bean ne sera pas enregistré, donc l'app refusera de démarrer si quelque chose
  dépend de ce service. L'erreur est immédiate et visible.
- Tous les beans du domaine et de la persistance doivent passer par `@Bean` dans
  `config/` — c'est imposé par la structure, pas juste par convention.
- Les controllers (`@RestController` dans `infrastructure/http/`) et les classes
  `@Configuration` (dans `config/`) restent scannés normalement.

Cette restriction est une **garantie statique** qui tient la discipline dans le temps,
y compris quand le projet grossit ou que d'autres personnes contribuent.

---

### Structure des packages

```
school.charset.app/
├── domain/
│   ├── model/                          # Data classes pures
│   │   ├── CodePoint.kt
│   │   ├── Encoding.kt                 # sealed class Encoding (Utf8, Utf16, Latin1, ...)
│   │   ├── ExerciseModule.kt
│   │   ├── ExerciseLevel.kt
│   │   ├── ValidationResult.kt
│   │   ├── ModuleProgress.kt
│   │   ├── ExerciseAttempt.kt
│   │   └── User.kt
│   ├── port/                           # Interfaces (contrats)
│   │   ├── UserRepository.kt
│   │   ├── ProgressRepository.kt
│   │   ├── ExerciseAttemptRepository.kt
│   │   └── Clock.kt
│   └── service/                        # Logique métier (classes Kotlin pures)
│       ├── EncodingService.kt
│       ├── ExerciseGenerator.kt
│       ├── AnswerValidator.kt
│       └── ProgressService.kt
│
├── infrastructure/
│   ├── persistence/                    # Implémentations Exposed (sans annotation)
│   │   ├── tables/                     # objects Exposed (UsersTable, etc.)
│   │   ├── mappers/                    # row → domain model
│   │   ├── ExposedUserRepository.kt
│   │   ├── ExposedProgressRepository.kt
│   │   └── ExposedExerciseAttemptRepository.kt
│   ├── http/                           # Controllers + DTOs
│   │   ├── auth/
│   │   ├── exercise/
│   │   ├── progress/
│   │   ├── dto/                        # data classes pour requêtes/réponses HTTP
│   │   └── serialization/              # serializers/deserializers Jackson custom
│   ├── security/                       # Spring Security config + UserDetailsService
│   └── time/
│       └── SystemClock.kt
│
├── config/                             # Wiring Spring centralisé
│   ├── DomainConfig.kt                 # @Bean des services domain
│   ├── PersistenceConfig.kt            # @Bean des repositories + Database Exposed
│   ├── InfrastructureConfig.kt         # @Bean Clock et autres utilitaires
│   ├── JacksonConfig.kt                # ObjectMapper + modules custom
│   ├── SecurityConfig.kt               # SecurityFilterChain, UserDetailsService, etc.
│   └── WebConfig.kt                    # CORS, MessageSource, etc.
│
└── CharsetApplication.kt               # @SpringBootApplication (scan limité à config/, http/)
```

### Exemple — domaine pur

```kotlin
// domain/model/ModuleProgress.kt — aucune annotation, aucune dépendance framework
package school.charset.app.domain.model

import kotlinx.datetime.Instant

data class ModuleProgress(
    val userId: Long,
    val moduleId: String,
    val level: Int,
    val streak: Int,
    val attempts: Int,
    val errors: Int,
    val lastPlayedAt: Instant?,
) {
    fun recordAttempt(correct: Boolean, now: Instant): ModuleProgress = copy(
        attempts = attempts + 1,
        errors = if (correct) errors else errors + 1,
        streak = if (correct) streak + 1 else 0,
        lastPlayedAt = now,
    )

    companion object {
        fun initial(userId: Long, moduleId: String): ModuleProgress =
            ModuleProgress(userId, moduleId, 1, 0, 0, 0, null)
    }
}
```

```kotlin
// domain/port/ProgressRepository.kt — contrat pur
package school.charset.app.domain.port

interface ProgressRepository {
    fun findByUserAndModule(userId: Long, moduleId: String): ModuleProgress?
    fun upsert(progress: ModuleProgress): ModuleProgress
}
```

```kotlin
// domain/service/ProgressService.kt — logique métier, classe Kotlin pure
package school.charset.app.domain.service

class ProgressService(
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
) {
    fun recordAttempt(userId: Long, moduleId: String, correct: Boolean): ModuleProgress {
        val current = progressRepository.findByUserAndModule(userId, moduleId)
            ?: ModuleProgress.initial(userId, moduleId)
        return progressRepository.upsert(current.recordAttempt(correct, clock.now()))
    }
}
```

### Exemple — wiring Spring

```kotlin
// config/DomainConfig.kt
@Configuration
class DomainConfig {
    @Bean
    fun progressService(
        progressRepository: ProgressRepository,
        clock: Clock,
    ): ProgressService = ProgressService(progressRepository, clock)

    @Bean
    fun answerValidator(): AnswerValidator = AnswerValidator()

    @Bean
    fun exerciseGenerator(clock: Clock): ExerciseGenerator = ExerciseGenerator(clock)
}

// config/PersistenceConfig.kt
@Configuration
class PersistenceConfig {

    @Bean
    fun database(dataSource: DataSource): Database =
        Database.connect(
            dataSource,
            databaseConfig = DatabaseConfig { useNestedTransactions = true },
        )

    @Bean
    fun userRepository(database: Database): UserRepository =
        ExposedUserRepository(database)

    @Bean
    fun progressRepository(database: Database): ProgressRepository =
        ExposedProgressRepository(database)

    @Bean
    fun exerciseAttemptRepository(database: Database): ExerciseAttemptRepository =
        ExposedExerciseAttemptRepository(database)
}

// config/InfrastructureConfig.kt
@Configuration
class InfrastructureConfig {
    @Bean
    fun clock(): Clock = SystemClock()
}
```

### Transactions

Préférence : **transactions Exposed explicites** dans les repositories (`transaction(database) { }`),
**pas** `@Transactional` Spring. Cohérent avec l'approche "pas d'annotations Spring qui leakent",
et plus simple à raisonner (pas de mélange entre contexte transactionnel Spring et Exposed).

---

## Sérialisation JSON — Jackson 3 sans pollution du domaine

### Stratégie

- `jackson-module-kotlin` gère les data classes Kotlin out of the box (defaults, nullables,
  `data class`, sealed classes)
- **Aucune annotation Jackson dans `domain/model/`**
- Serializers/deserializers custom dans `infrastructure/http/serialization/`, enregistrés
  via un `SimpleModule` dans `JacksonConfig`
- Module custom pour `kotlinx.datetime.Instant` (sérialisation ISO-8601)

### Exemple

```kotlin
// infrastructure/http/serialization/ModuleProgressSerializer.kt
package school.charset.app.infrastructure.http.serialization

class ModuleProgressSerializer : JsonSerializer<ModuleProgress>() {
    override fun serialize(value: ModuleProgress, gen: JsonGenerator, sp: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("moduleId", value.moduleId)
        gen.writeNumberField("level", value.level)
        gen.writeNumberField("streak", value.streak)
        gen.writeNumberField("attempts", value.attempts)
        gen.writeNumberField("errors", value.errors)
        value.lastPlayedAt?.let { gen.writeStringField("lastPlayedAt", it.toString()) }
        gen.writeEndObject()
    }
}

// config/JacksonConfig.kt
@Configuration
class JacksonConfig {

    @Bean
    fun domainSerializationModule(): Module = SimpleModule().apply {
        addSerializer(ModuleProgress::class.java, ModuleProgressSerializer())
        // autres custom serializers
    }

    @Bean
    fun kotlinxDatetimeModule(): Module = SimpleModule().apply {
        addSerializer(Instant::class.java, InstantSerializer())
        addDeserializer(Instant::class.java, InstantDeserializer())
    }

    @Bean
    fun jacksonObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.modulesToInstall(
                kotlinModule(),
                domainSerializationModule(),
                kotlinxDatetimeModule(),
            )
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
}
```

### Tests des serializers

Tests unitaires purs, sans Spring :

```kotlin
class ModuleProgressSerializerTest : FunSpec({
    val mapper = jacksonObjectMapper().apply {
        registerModule(SimpleModule().addSerializer(
            ModuleProgress::class.java,
            ModuleProgressSerializer(),
        ))
    }

    test("serializes all fields") {
        val progress = ModuleProgress(1L, "utf8-encode", 3, 5, 50, 12, null)
        val json = mapper.writeValueAsString(progress)
        json shouldContain """"moduleId":"utf8-encode""""
        json shouldContain """"level":3"""
    }

    test("omits lastPlayedAt when null") {
        val progress = ModuleProgress(1L, "utf8-encode", 1, 0, 0, 0, null)
        val json = mapper.writeValueAsString(progress)
        json shouldNotContain "lastPlayedAt"
    }
})
```

---

## Modèle de données

### `users`
```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    locale        VARCHAR(5)   NOT NULL DEFAULT 'fr',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### `module_progress`
```sql
CREATE TABLE module_progress (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id      VARCHAR(64)  NOT NULL,
    level          SMALLINT     NOT NULL DEFAULT 1,
    streak         INT          NOT NULL DEFAULT 0,
    attempts       INT          NOT NULL DEFAULT 0,
    errors         INT          NOT NULL DEFAULT 0,
    last_played_at TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, module_id)
);
```

### `exercise_attempts` (optionnel v1)
```sql
CREATE TABLE exercise_attempts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id   VARCHAR(64)  NOT NULL,
    level       SMALLINT     NOT NULL,
    input       JSONB        NOT NULL,
    expected    JSONB        NOT NULL,
    user_answer JSONB        NOT NULL,
    correct     BOOLEAN      NOT NULL,
    duration_ms INT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_attempts_user_module ON exercise_attempts(user_id, module_id);
```

### Tables Spring Session JDBC
Générées via le script officiel `schema-postgresql.sql` dans une migration Flyway.

---

## Modules d'exercices

### 1. Encoder en UTF-8 (priorité 1)
Code point → bytes UTF-8, 5 étapes : choisir le format, écrire le binaire, découper bits
hauts/bas, remplir le format avec marqueurs, convertir en hex.

### 2. Décoder UTF-8 — bytes → code point
### 3. Encoder en UTF-16 (avec endianness)
### 4. Décoder UTF-16 (avec BOM)
### 5. Encoder en UTF-32 (avec endianness)
### 6. Décoder UTF-32 (avec BOM)
### 7. Endianness — module dédié au BOM et à l'ordre des bytes
### 8. Mojibake — `Ã©` → `é` + identifier les encodages
### 9. Identifier l'encodage — séquence de bytes → encodages possibles

### Difficulté graduée (par module)
- Niveau 1 : ASCII / cas simples
- Niveau 2 : Latin-1 / 2 bytes
- Niveau 3 : CJK / 3 bytes
- Niveau 4 : Emojis / 4 bytes
- Niveau 5 : Random sur toute la plage Unicode

---

## API REST (Spring Boot)

### Auth
- `POST /api/auth/register` — inscription
- `POST /api/auth/login` — connexion (crée session, écrit cookie HttpOnly)
- `POST /api/auth/logout` — invalide session
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET  /api/auth/me`

### Exercices
- `POST /api/exercise/generate` — génère un exercice (sans révéler la solution)
- `POST /api/exercise/validate` — valide une tentative, met à jour la progression
- `GET  /api/progress`

### Profil
- `PATCH  /api/profile`
- `PATCH  /api/profile/password`
- `DELETE /api/profile`

Tous les endpoints (sauf `/api/auth/register`, `/login`, `/forgot-password`,
`/reset-password`) nécessitent l'authentification.

---

## Authentification

### Stratégie
Sessions Spring Security avec cookie HttpOnly, SameSite=Strict, Secure (en prod).
Pas de JWT.

### CSRF
Activé. Spring émet un cookie `XSRF-TOKEN` (non HttpOnly, lisible par JS). Le frontend
(via un interceptor TanStack Query) lit ce cookie et le renvoie en header `X-XSRF-TOKEN`.

### CORS
Configuré pour `credentials: include`. En prod : même domaine via Caddy = pas de CORS.
En dev : `localhost:3000` → `localhost:8080`.

### Bcrypt
Force 12 (par défaut Spring Security = 10, on monte un peu).

### Reset password
- Token random 32 bytes, hash bcrypt stocké en DB avec expiration 1h
- Envoi par email (Mailpit en dev, vrai SMTP en prod)
- Une fois utilisé, le token est invalidé

---

## Internationalisation

### Backend
`MessageSource` Spring pour les emails et messages de validation. Fichiers
`messages_fr.properties` et `messages_en.properties` dans `src/main/resources/`.

### Frontend
i18next + react-i18next. Namespaces : `common`, `auth`, `landing`, `exercise`, `modules`, `feedback`.

### Synchronisation
La locale courante est dans `users.locale` (DB) ou cookie `locale` (invités). Renvoyée
dans `GET /api/auth/me`.

### Hints découplés du domaine
`domain/service/AnswerValidator` retourne un `ValidationResult` avec `errorType` + `params` :

```kotlin
data class ValidationResult(
    val ok: Boolean,
    val expected: String,
    val errorType: String? = null,
    val params: Map<String, String> = emptyMap(),
)
```

Le frontend traduit `errorType` via i18next avec interpolation des `params`.

---

## Tests

### Tests unitaires domain (Kotest + MockK)

```kotlin
class ProgressServiceTest : FunSpec({
    val repo = mockk<ProgressRepository>()
    val clock = mockk<Clock>()
    val service = ProgressService(repo, clock)

    test("records first attempt as success") {
        every { clock.now() } returns Instant.parse("2026-01-01T00:00:00Z")
        every { repo.findByUserAndModule(1, "utf8-encode") } returns null
        every { repo.upsert(any()) } answers { firstArg() }

        val result = service.recordAttempt(1, "utf8-encode", correct = true)

        result.attempts shouldBe 1
        result.errors shouldBe 0
        result.streak shouldBe 1
    }
})
```

### Tests d'intégration ciblés (Testcontainers)

Tu peux n'importer que la config nécessaire pour gagner en vitesse :

```kotlin
@SpringBootTest(classes = [PersistenceConfig::class])
@Testcontainers
class ExposedProgressRepositoryIntegrationTest : FunSpec({
    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:18-alpine")
    }
    // tests
})
```

### Tests de serializers (purs, sans Spring)
Voir section "Sérialisation JSON" ci-dessus.

### Couverture minimale obligatoire
- `domain/service/EncodingService` : tous les cas frontières (U+007F, U+0080, U+07FF,
  U+0800, U+FFFF, U+10000, U+10FFFF, surrogates UTF-16)
- `domain/service/AnswerValidator` : chaque `errorType` produit avec les bons `params`
- `infrastructure/persistence/*` : upsert, find, contraintes uniques
- Controllers : 401 si non auth, 403 si mauvais user, 200 si OK
- Serializers custom : structure JSON exacte

---

## Configuration Caddy

### Dev local

```caddyfile
charset.localhost {
    reverse_proxy /api/* localhost:8080
    reverse_proxy localhost:3000
}
```

### Production

```caddyfile
charset.school {
    # API Spring Boot
    reverse_proxy /api/* localhost:8080

    # SSR TanStack Start (Node)
    reverse_proxy localhost:3000

    # Headers de sécurité
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "strict-origin-when-cross-origin"
    }

    # Compression
    encode gzip zstd
}
```

Caddy gère HTTPS automatiquement via Let's Encrypt sur le domaine `charset.school`.

---

## Infrastructure / Hébergement

### Domaine
- **charset.school** (acheté via Porkbun / Namecheap)
- DNS pointant vers l'IPv4 du VPS

### Repo GitHub
- **Compte personnel**, repo `charset-school`
- Migration possible vers une org plus tard si le projet décolle
- Branche principale `main`, protection de branche optionnelle pour le solo

### VPS production
- **4 vCores, 8 Go RAM, 75 Go SSD, trafic illimité, 400 Mbit/s**
- Datacenter EU (RGPD + latence)
- ~7 €/mois
- IPv4 dédiée incluse

### Stack serveur
- Ubuntu LTS récent
- **Caddy** (reverse proxy + HTTPS auto via Let's Encrypt)
- **Java 25** (ou JDK 21 LTS si tu préfères stable) pour Spring Boot
- **Node 22+** pour TanStack Start SSR
- **Docker Compose** uniquement pour Postgres 18 (isolation simple)
- **systemd** units pour Spring Boot et TanStack Start

### Sécurité serveur (premier setup)
- Utilisateur non-root avec clé SSH uniquement
- `PermitRootLogin no`, `PasswordAuthentication no`
- `ufw` : ports 22, 80, 443 uniquement
- `fail2ban` contre les bruteforce SSH
- `unattended-upgrades` pour les patches de sécurité auto

### Postgres tuning (postgresql.conf)
```conf
shared_buffers = 2GB
effective_cache_size = 4GB
work_mem = 16MB
maintenance_work_mem = 256MB
```

Avec 8 Go de RAM, ces réglages permettent à Postgres de garder l'essentiel des données
chaudes en cache.

### Backups
- `pg_dump` quotidien via cron
- Upload vers stockage S3-compatible (Backblaze B2 ou Cloudflare R2)
- Rétention : 7 jours quotidiens + 4 hebdomadaires
- Test de restauration mensuel

### Monitoring
- **Spring Boot Actuator** (`/actuator/health`, `/actuator/metrics`)
- Caddy log files rotatés
- Spring Boot logs avec `logback` et rotation
- Healthcheck Caddy → endpoint `/actuator/health` toutes les 30s

---



### Phase 1 — Fondations domain
1. Setup projet Gradle Kotlin DSL, Spring Boot 4 starter, dépendances de base
2. `domain/model/` + `domain/port/` (data classes et interfaces, zéro dépendance framework)
3. `domain/service/EncodingService` avec tests Kotest complets (frontières UTF-8/16/32,
   surrogates, BOM)
4. `domain/service/AnswerValidator` et `ExerciseGenerator` avec tests
5. `config/DomainConfig` pour le wiring `@Bean` des services

### Phase 2 — Infrastructure DB
6. Postgres + Flyway (autoconfig), migrations `users`, `module_progress`,
   `exercise_attempts`, tables Spring Session
7. `infrastructure/persistence/` : tables Exposed + repositories (classes Kotlin pures)
8. `config/PersistenceConfig` pour le wiring `@Bean` des repositories + `Database` Exposed
9. Tests d'intégration Testcontainers ciblés sur `PersistenceConfig`

### Phase 3 — Sécurité + Auth
10. `config/SecurityConfig` : `SecurityFilterChain`, password encoder, etc.
11. `infrastructure/security/` : `UserDetailsService` basé sur `UserRepository`
12. Controllers `/api/auth/*` (`@RestController`)
13. Spring Session JDBC activé via `@EnableJdbcHttpSession`
14. Tests d'intégration auth (login, logout, register, reset password)

### Phase 4 — Sérialisation + API
15. `config/JacksonConfig` : `ObjectMapper` avec modules custom (kotlin, kotlinx-datetime,
    serializers domaine)
16. `infrastructure/http/serialization/` : serializers custom + tests purs
17. Controllers `/api/exercise/*` et `/api/progress`
18. Validation des DTOs HTTP via Bean Validation

### Phase 5 — Frontend setup
19. `npm create @tanstack/start@latest` + TypeScript + Tailwind v4 + shadcn
20. TanStack Query setup + client API typé (zod sur les responses)
21. i18next setup avec namespaces FR/EN
22. Layouts `AuthLayout` et `AppLayout`, header global

### Phase 6 — Auth UI
23. Pages Login, Register, ForgotPassword, ResetPassword (TanStack Form + zod + shadcn)
24. Page Profile avec sections (compte, langue, password, danger zone)

### Phase 7 — Exercices
25. Composants atomiques : `BitDisplay`, `BitInput`, `HexInput`, `ByteDisplay`,
    `FormatSelector`, `EndiannessSelector`, `StepProgress`, `FeedbackPanel`
26. Module **Encoder en UTF-8** complet (niveaux 1 à 5) — le module phare
27. Module **Décoder UTF-8**
28. Modules UTF-16 encode/decode
29. Modules UTF-32 encode/decode
30. Module Endianness
31. Module Mojibake
32. Module Identifier l'encodage

### Phase 8 — Landing + polish
33. Landing mode invité (hero + CTAs)
34. Landing mode connecté (cartes modules + progression)
35. Random total
36. Thèmes light/dark finalisés
37. Responsive mobile complet
38. Audit accessibilité (clavier, lecteur d'écran)

### Phase 9 — Déploiement
39. Dockerfile Spring Boot
40. Build TanStack Start en mode SSR Node
41. Caddyfile production
42. Migrations en CI/CD
43. Monitoring basique (Spring Boot Actuator + logs)

### À envisager plus tard (post-v1)
- `@ControllerAdvice` pour la gestion centralisée des erreurs HTTP
- OpenAPI (Springdoc) si tu veux exposer la doc d'API
- Cache Caffeine côté Spring si nécessaire
- Rate limiting (Bucket4j ou Resilience4j)

À chaque phase :
- `./gradlew test` et `npm test` doivent passer
- `./gradlew ktlintCheck` ou detekt sans warning
- ESLint + Prettier propres côté front

---

## Anti-cheat (léger)

`POST /api/exercise/generate` retourne uniquement l'énoncé (code point ou bytes),
**pas la solution**. La validation se fait côté serveur via `AnswerValidator`.

Pour empêcher de modifier l'énoncé entre génération et validation :
- L'exercice généré contient un `attemptId` (UUID) stocké en session HTTP
- `POST /api/exercise/validate` doit fournir cet `attemptId`
- Le serveur recalcule l'attendu depuis l'input stocké en session
- Une fois validé, l'`attemptId` est consommé (un seul submit possible)

---

## Non-objectifs (v1)

- Pas de leaderboard public
- Pas de social (followers, profil public, partage)
- Pas d'OAuth (Google/GitHub)
- Pas de 2FA
- Pas d'app mobile native — PWA éventuelle plus tard
- Pas de paiement / premium
- Pas de microservices ni de CQRS — monolithe Spring Boot
- Pas de Server Components React
- Pas de cache Redis tant que pas nécessaire
- Pas de `@ControllerAdvice` ni OpenAPI tant que le squelette n'est pas en place
