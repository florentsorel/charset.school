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
- **Exposed** v1.3+ (DSL fluide) avec **exposed-kotlin-datetime** pour le mapping
  `kotlin.time.Instant` ↔ colonnes Postgres `TIMESTAMP`
- **Postgres 18** + **Flyway** pour les migrations (SQL pur, autoconfig Spring Boot) +
  `flyway-database-postgresql` en `runtimeOnly` (requis en Flyway 11.x pour le support Postgres)
- **Bean Validation** (Hibernate Validator) pour la validation des inputs côté HTTP
- **`kotlin.time.Instant`** (stdlib Kotlin 2.x) pour les timestamps en domaine. `kotlinx.datetime.Instant`
  est un typealias déprécié vers stdlib — on importe directement `kotlin.time.*`. ISO-8601
  sur la wire via `Instant.toString()`
- **Jackson 3** + `jackson-module-kotlin` (sérialisation JSON sans pollution du domaine)
- **Testcontainers** (Postgres) — démarré en `companion object` via `@Container` + injecté
  dans Spring via `@DynamicPropertySource`
- **Tests d'intégration** : JUnit 5 direct (`@Test` `org.junit.jupiter.api`) — nécessaire pour
  `@SpringBootTest` + extensions Testcontainers
- **Tests unitaires** : **Kotest** spec BDD-style :
  - `FunSpec` pour des tests plats (1-N cas indépendants — ex. `UserSerializerTest`)
  - `FreeSpec` quand on a un groupement par méthode (ex. service avec plusieurs méthodes
    ayant chacune leurs cas — `"methodName" - { "case description" { ... } }`)
  - **Pas d'`AnnotationSpec`** — autant utiliser JUnit directement si on veut `@Test fun`
- **MockK** pour les mocks Kotlin idiomatiques
- **Gradle 9** Kotlin DSL

### Frontend
- **Nuxt 4** (Vue 3 + Vite + SSR activé, file-based routing, server routes possibles si besoin).
  Structure Nuxt 4 : sources sous `web/app/` (`app/pages/`, `app/components/`, `app/composables/`, etc.)
- **Vue 3** Composition API avec `<script setup>` + **TypeScript** (strict)
- **`useFetch` / `$fetch`** (built-in Nuxt) pour les appels API vers Spring Boot, avec
  intercepteur global pour `credentials: 'include'` + injection du header `X-XSRF-TOKEN`
- **VeeValidate + zod** pour les formulaires (validation déclarative, type-safe)
- **Tailwind CSS v4** (`@theme` dans CSS, intégré via Nuxt UI)
- **Nuxt UI v4** (composants Vue prêts à l'emploi, theming Tailwind v4 natif, accessibilité,
  inclut `@iconify-json/lucide` pour les icônes Lucide)
- **Pinia** uniquement si nécessaire (user courant, locale, progression) — partir sans,
  ajouter quand le besoin de state cross-vue mutable apparaît réellement
- **zod** pour valider les schémas des réponses API (en plus de VeeValidate côté forms)
- **`@nuxtjs/i18n`** (FR + EN, FR par défaut, détection via cookie ou `users.locale`)
- **pnpm** comme package manager (déclaré dans `packageManager` field). CI cache la `pnpm-store`

### Reverse proxy / déploiement
- **Caddy** comme reverse proxy unique (HTTPS auto via Let's Encrypt, proxy de Nuxt SSR
  Node sur :3000, proxy `/api/*` vers Spring Boot sur :8080)
- Spring Boot tourne en JAR exécutable derrière Caddy
- Nuxt en mode SSR (`nuxt build` puis `node .output/server/index.mjs`) pour le SEO sur
  la landing et toutes les pages publiques

---

## Architecture — domain / infrastructure

Pattern **ports & adapters léger** (pas de DDD strict, pas de CQRS).

### Principes

1. **Le domaine est pur Kotlin.** Pas de Spring, pas de Jackson, pas d'Exposed, pas
   d'annotation framework. Uniquement le stdlib Kotlin (incluant `kotlin.time.*` pour
   les timestamps et le clock).
2. **Le domaine ne définit que ses contrats.** Les interfaces (ex. `*Repository`,
   `Clock`) vivent dans le package de la feature concernée (`domain/exercise/`,
   `domain/progress/`, etc., **pas** dans un dossier `port/` à part). Les
   implémentations concrètes vivent dans `infrastructure/`.
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
| `domain/<feature>/*Service`, `*Validator`, `*Generator`, `Codec`, `*Hasher` | `@Bean` dans le **`<Feature>Config`** correspondant (ex. `AuthConfig` héberge `authService` + `passwordHasher`) | Cohérent avec l'archi feature-first |
| `domain/<feature>/*Repository` (interfaces) | rien (interface) | C'est un contrat |
| `infrastructure/repository/<feature>/*` + `infrastructure/http/<feature>/serde/*` | `@Bean` dans le même **`<Feature>Config`** (regroupe repo + serializers + autres beans liés à l'entité, façon nbog) | Cohérent — tout ce qui touche à `User` vit dans `UserConfig`, etc. |
| `Database` Exposed + `DataSource` | `@Bean` dans `DatabaseConfig` | Plomberie partagée par tous les repos, pas spécifique à un feature |
| `infrastructure/http/*Controller` | `@RestController` direct | Intrinsèquement Spring |
| `infrastructure/security/*` | `@Bean` dans `SecurityConfig` | Centralisation du wiring sécurité |
| `kotlin.time.Clock` (stdlib) | `@Bean clock(): Clock = Clock.System` dans `ApplicationConfig` (+ `@PostConstruct setTz("UTC")`) | Pas d'abstraction custom — MockK mocke directement `Clock`. `Clock.System` n'est qu'une instance, le port est déjà dans la stdlib |
| `JacksonModule` (un par feature) | `@Bean userJacksonModule()` dans `UserConfig`, idem `ExerciseConfig`, etc. Spring Boot collecte **tous** les beans `JacksonModule` et les enregistre sur l'ObjectMapper auto-configuré | Pattern marker bean — pas de registre central à maintenir |
| `config/*Config` | `@Configuration` | C'est leur rôle |
| `DataSource` | Autoconfig Spring Boot via `application.yml` | Pas de raison de réinventer |
| Flyway | Autoconfig Spring Boot | Idem |
| Spring Session JDBC | `@EnableJdbcHttpSession` sur `SecurityConfig` | Annotation Spring de config |

### Restriction du component scan

Spring Boot fait du component scan par défaut sur le package racine de
`@SpringBootApplication`. Pour garantir qu'aucune annotation Spring (`@Service`,
`@Repository`, `@Component`) ne soit accidentellement scannée dans `domain/` ou
`infrastructure/repository/`, on restreint explicitement le scan à `config/` et
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
├── domain/                             # Pure Kotlin, zéro dépendance framework.
│   │                                   # Organisé par **feature/entité**, pas par couche
│   │                                   # technique (pas de `model/` / `service/` / `port/`).
│   │                                   # Inspiré du package `bl/` du projet widder.
│   │
│   ├── encoding/                       # Tout ce qui concerne les encodages
│   │   ├── CodePoint.kt                # @JvmInline value class Int wrapper
│   │   ├── Encoding.kt                 # enum (Ascii, Latin1, Windows1252, Utf8, Utf16Be, ...)
│   │   ├── ByteArrayExt.kt             # extension toHex()
│   │   ├── Codec.kt                    # encode/decode pour les 8 encodings
│   │   ├── EncoderException.kt
│   │   └── DecoderException.kt
│   │
│   ├── exercise/                       # Tout ce qui concerne les exercices et leur validation
│   │   ├── Granularity.kt              # enum Verbose / Standard / Compact
│   │   ├── StepType.kt                 # enum Format / Binary / BitGroups / HexBytes / CodePointEntry / Endianness
│   │   ├── Step.kt                     # sealed class — un step par valeur de StepType
│   │   ├── Answer.kt                   # sealed class — un Answer par type de step
│   │   ├── ValidationResult.kt         # data class (ok, errorType?, params) — pas d'expected (anti-cheat)
│   │   ├── ErrorType.kt                # object — identifiants stables des erreurs de validation
│   │   ├── ParamKey.kt                 # object — noms des variables d'interpolation
│   │   ├── FormatChoice.kt             # object — identifiants stables des choix Step.Format (byte-count.*)
│   │   ├── Exercise.kt                 # data class (codePoint, encoding, granularity, steps)
│   │   ├── ExerciseAttempt.kt          # data class (id, userId, moduleId, level, granularity, steps, correct, ...)
│   │   ├── ExerciseModule.kt           # enum des modules (utf8-encode, utf8-decode, ...)
│   │   ├── ExerciseLevel.kt
│   │   ├── ExerciseAttemptRepository.kt  # interface (port)
│   │   ├── AnswerValidator.kt          # class — validate(step: Step, answer: Answer) -> ValidationResult
│   │   └── generator/                  # ExerciseGenerator + per-encoding generators (see "Generators" section)
│   │
│   ├── progress/                       # Tout ce qui concerne la progression utilisateur
│   │   ├── ModuleProgress.kt           # data class (userId, moduleId, level, streak, attempts, errors, lastPlayedAt)
│   │   ├── ProgressRepository.kt       # interface (port)
│   │   └── ProgressService.kt          # class — record d'une tentative + mise à jour de la progression
│   │
│   └── user/                           # Tout ce qui concerne les utilisateurs
│       ├── User.kt                     # data class (id, email, name, locale, ...)
│       └── UserRepository.kt           # interface (port)
│   # Pour les besoins temporels (timestamps, etc.), on injecte directement
│   # `kotlin.time.Clock` (stdlib) — pas d'interface `Clock` custom dans `domain/time/`.
│   # MockK mocke `Clock` natif sans effort, et le bean est fourni par
│   # `config/ApplicationConfig.kt`.
│   #
│   # Note : les constantes "stables vers le front" (ErrorType, ParamKey, plus tard
│   # HintType, MessageType, ...) vivent **dans le package du feature qui les produit**,
│   # pas dans un package i18n/ transverse. Cohérent avec l'archi feature-first.
│
├── infrastructure/                     # Le seul endroit où on a un mix Spring/Exposed/Jackson.
│   │                                   # Top-level split par couche technique (repository, http,
│   │                                   # security, time), puis feature-package À L'INTÉRIEUR
│   │                                   # de chaque couche quand c'est pertinent.
│   │
│   ├── repository/                    # Implémentations Exposed (sans annotation Spring)
│   │   ├── exercise/                   # ExposedExerciseAttemptRepository + tables +
│   │   │                               # mappers pour Step → table fille
│   │   ├── progress/                   # ExposedProgressRepository + ModuleProgressTable
│   │   └── user/                       # ExposedUserRepository + UsersTable
│   │
│   ├── http/                           # Controllers + DTOs + serializers (déjà feature-packagé)
│   │   ├── auth/
│   │   ├── exercise/
│   │   ├── progress/
│   │   ├── profile/
│   │   └── serialization/              # serializers/deserializers Jackson custom transverses
│   │
│   └── security/                       # Spring Security config + UserDetailsService
│
├── config/                             # Wiring Spring centralisé, **per-entity**
│   ├── ApplicationConfig.kt            # @Bean Clock (= Clock.System) + @PostConstruct setTz(UTC)
│   ├── DatabaseConfig.kt               # @Bean DataSource + @Bean Database Exposed (plomberie)
│   ├── SecurityConfig.kt               # SecurityFilterChain, UserDetailsService, AuthenticationManager, RememberMeServices, etc.
│   ├── AuthConfig.kt                   # @Bean authService + passwordHasher (feature auth)
│   ├── UserConfig.kt                   # @Bean userRepository + userJacksonModule (feature user)
│   ├── ProgressConfig.kt               # @Bean progressRepository + progressService + progressJacksonModule (Phase 4)
│   ├── ExerciseConfig.kt               # @Bean exerciseAttemptRepository + exerciseGenerator + exerciseJacksonModule (Phase 4)
│   └── WebConfig.kt                    # CORS, MessageSource, etc.
│
└── CharsetApplication.kt               # @SpringBootApplication (scan limité à config/, http/)
```

### Exemple — domaine pur

Data class, interface (port) et service de la **même feature** vivent dans le
**même package** (`domain/progress/`) :

```kotlin
// domain/progress/ModuleProgress.kt — aucune annotation, aucune dépendance framework
package school.charset.app.domain.progress

import kotlin.time.Instant

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
// domain/progress/ProgressRepository.kt — contrat pur, même package que l'entité
package school.charset.app.domain.progress

interface ProgressRepository {
    fun findByUserAndModule(userId: Long, moduleId: String): ModuleProgress?
    fun upsert(progress: ModuleProgress): ModuleProgress
}
```

```kotlin
// domain/progress/ProgressService.kt — logique métier, classe Kotlin pure
package school.charset.app.domain.progress

import kotlin.time.Clock

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
// config/ProgressConfig.kt (Phase 5) — service domain + repo regroupés par entité
@Configuration
class ProgressConfig {
    @Bean
    fun progressRepository(): ProgressRepository = ExposedProgressRepository()

    @Bean
    fun progressService(
        progressRepository: ProgressRepository,
        clock: Clock,
    ): ProgressService = ProgressService(progressRepository, clock)

    @Bean
    fun progressJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(ProgressSerializer())
    }
}

// config/DatabaseConfig.kt — plomberie partagée par tous les repos
@Configuration
@EnableConfigurationProperties(DataSourceProperties::class)
class DatabaseConfig {

    @Bean
    fun dataSource(properties: DataSourceProperties): DataSource =
        DataSourceBuilder.create()
            .driverClassName(properties.driverClassName)
            .url(properties.url)
            .username(properties.username)
            .password(properties.password)
            .build()

    @Bean
    fun database(dataSource: DataSource): Database =
        Database.connect(
            dataSource,
            databaseConfig = ExposedDatabaseConfig { useNestedTransactions = true },
        )
}

// config/UserConfig.kt — un fichier par entité, regroupe TOUT ce qui touche à User
@Configuration
class UserConfig {
    @Bean
    fun userRepository(clock: Clock): UserRepository = ExposedUserRepository(clock)

    @Bean
    fun userJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(UserSerializer())
    }
}

// config/AuthConfig.kt — un fichier pour la feature auth (entity-agnostic)
@Configuration
class AuthConfig {
    @Bean
    fun passwordHasher(passwordEncoder: PasswordEncoder): PasswordHasher =
        BCryptPasswordHasher(passwordEncoder)

    @Bean
    fun authService(userRepository: UserRepository, passwordHasher: PasswordHasher): AuthService =
        AuthService(userRepository, passwordHasher)
}

// config/ExerciseConfig.kt (Phase 4) — repo + générateurs + serializer Step
@Configuration
class ExerciseConfig {
    @Bean
    fun exerciseAttemptRepository(): ExerciseAttemptRepository =
        ExposedExerciseAttemptRepository()

    @Bean
    fun exerciseGenerator(clock: Clock): ExerciseGenerator = ExerciseGenerator(clock)

    @Bean
    fun exerciseJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(StepSerializer())  // strip `expected` anti-cheat
    }
}

// config/ApplicationConfig.kt — bean Clock (stdlib) + TZ forcé en UTC
@Configuration
class ApplicationConfig {

    @PostConstruct
    fun setTz() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Bean
    fun clock(): Clock = Clock.System  // kotlin.time.Clock
}
```

### Transactions

Dans les repositories, on utilise la DSL Exposed `transaction { }` (sans argument).
Le `Database` est enregistré comme **default global** par `Database.connect(dataSource)` dans
`DatabaseConfig.database()` au démarrage Spring (le bean est instancié eagerly), donc tous
les `transaction { }` ultérieurs partagent ce default. Plus besoin d'injecter `Database`
dans chaque repo, le constructeur reste minimal (juste les vraies dépendances métier comme
`Clock`).

**Pas de `@Transactional` Spring** dans les repos :

- Spring `@Transactional` et Exposed `transaction { }` sont deux systèmes de transaction
  distincts. Spring ouvre une transaction via `PlatformTransactionManager` ; Exposed via
  son `TransactionManager` interne. Sans bridge, Exposed ne voit pas la transaction Spring
  et lève "No transaction" à l'exécution.
- Le bridge officiel existe (`org.jetbrains.exposed:exposed-spring-transaction`) et serait
  une option **défendable** sur le plan archi — `infrastructure/repository/` est de
  l'infra, donc des annotations Spring n'y violent pas le principe (le principe protège
  uniquement le `domain/`). On l'évite pour des raisons pragmatiques, pas de pureté :
    - Une dépendance en plus
    - Frontières de transaction implicites (proxy Spring) vs explicites au call site
      (bloc `transaction { }` visible)
    - Match avec le pattern nrea déjà éprouvé
    - Pas un gain significatif : la DSL Exposed est aussi concise que l'annotation

---

## Sérialisation JSON — Jackson 3 sans pollution du domaine

### Stratégie

- `jackson-module-kotlin` gère les data classes Kotlin out of the box pour les **DTOs HTTP**
  (data classes dans `infrastructure/http/<feature>/`) — pas besoin de serializer custom
- Pour les **entités domain** qu'on retourne directement depuis un controller (`User`, `Step`...),
  on écrit un **`ValueSerializer<T>` Jackson 3 custom** dans `infrastructure/http/<feature>/serde/`.
  Pas de DTO intermédiaire, pas d'annotation Jackson sur le domaine, et **anti-leak par
  construction** — un champ non écrit ne peut pas leaker
- **Aucune annotation Jackson dans `domain/`** (ni dans `domain/exercise/`, ni `user/`, etc.)
- Les modules Jackson sont enregistrés **per-entité** via un `@Bean JacksonModule` dans
  le `<Feature>Config` correspondant. Spring Boot collecte tous les beans `JacksonModule`
  et les enregistre automatiquement sur l'ObjectMapper global

### Exemple

```kotlin
// infrastructure/http/auth/serde/UserSerializer.kt — Jackson 3 ValueSerializer
package school.charset.app.infrastructure.http.auth.serde

class UserSerializer : ValueSerializer<User>() {
    override fun serialize(user: User, gen: JsonGenerator, ctx: SerializationContext) {
        gen.writeStartObject()
        gen.writeNumberProperty("id", user.id)
        gen.writeStringProperty("email", user.email)
        gen.writeStringProperty("name", user.name)
        gen.writeStringProperty("locale", user.locale)
        gen.writeStringProperty("createdAt", user.createdAt.toString())
        // passwordHash et updatedAt **jamais** écrits → anti-leak by construction
        gen.writeEndObject()
    }

    override fun handledType(): Class<User> = User::class.java
}

// config/UserConfig.kt — bean JacksonModule co-localisé avec le userRepository
@Configuration
class UserConfig {
    @Bean
    fun userRepository(clock: Clock): UserRepository = ExposedUserRepository(clock)

    @Bean
    fun userJacksonModule(): JacksonModule = SimpleModule().apply {
        addSerializer(UserSerializer())
    }
}
```

Spring Boot trouve `userJacksonModule` (bean de type `JacksonModule`) et l'enregistre sur
l'ObjectMapper auto-configuré. Quand un `@RestController` retourne `ResponseEntity<User>`,
Jackson invoque automatiquement `UserSerializer`.

### Tests des serializers

Tests unitaires purs, sans Spring. Convention :
- **Kotest `FunSpec`** pour des tests plats (1-N cas indépendants)
- **Kotest `FreeSpec`** quand on a besoin de nesting (un service avec plusieurs méthodes)
- Helper de test partagé `ObjectMapperTestUtils.withSerializer(serializer)` dans `src/test/.../test/`
- Comparaison JSON par **tree-match** (`mapper.readTree(...) shouldBe mapper.readTree(...)`)
  pour ignorer whitespace/ordre. Test 2 valide l'anti-leak en passant des valeurs sensibles
  distinctives et asseratant que le tree complet correspond à la shape attendue.

```kotlin
class UserSerializerTest : FunSpec({
    val mapper = JsonMapper().withSerializer(UserSerializer())

    fun aUser(/* defaults */): User = ...

    fun User.serializeAndAssert(expectedJson: String) {
        val actual = mapper.writeValueAsString(this)
        mapper.readTree(actual) shouldBe mapper.readTree(expectedJson)
    }

    test("serializes id, email, name, locale and createdAt") { ... }
    test("never exposes passwordHash or updatedAt") {
        // passe un passwordHash distinctif puis assert que le JSON ne le contient pas
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

### Exercices — modèle relationnel pur (Option A : table-per-type)

**Choix d'archi tranché le 2026-05-20** : on stocke les attempts et leurs steps en
**relationnel pur**, pas en JSONB. Une table parent `exercise_attempts` agrège un
exercice complet ; une table parent `attempt_steps` agrège les micro-questions ; et
six tables filles (une par `StepType`) stockent les données spécifiques. Type-safe
DB-level, queries SQL natives faciles (stats par step_type), pas de JSON.

Le schéma doit **rester aligné** avec les objets domain (`Step` sealed class,
`StepType` enum, `Answer` sealed class, `Granularity` enum, `AnswerValidator`,
`ValidationResult`) — toute évolution du domaine doit refléter une migration et
inversement.

#### Pas de CHECK constraints sur les valeurs énumérées

**Choix tranché le 2026-05-20** : pas de `CHECK (... IN (...))` sur les colonnes
correspondant à des enums Kotlin (`granularity`, `step_type`, `encoding`, etc.).
Raisons :

- L'enum Kotlin est l'unique source de vérité ; la validation se fait au mapping
  Exposed (`enumerationByName`) — la DB n'a jamais à voir une valeur invalide
  écrite par l'app.
- Le coût d'évolution est asymétrique : ajouter une valeur Kotlin = 1 ligne ;
  faire la même chose avec CHECK = `ALTER TABLE DROP CONSTRAINT ... ADD CONSTRAINT ...`
  dans une migration Flyway. Bruit inutile.
- L'app est le seul writer de cette DB (pas d'ETL externe ni d'admin manuelle).

Les **FK** restent. Ce sont des contraintes d'intégrité référentielle, pas des
restrictions de valeurs.

#### `exercise_attempts`
```sql
CREATE TABLE exercise_attempts (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id     VARCHAR(64)  NOT NULL,
    level         SMALLINT     NOT NULL,
    granularity   VARCHAR(16)  NOT NULL,    -- 'verbose' | 'standard' | 'compact' (validé côté app)
    code_point    INT          NOT NULL,    -- input du module encode (et identifiant pour le module decode)
    encoding      VARCHAR(16)  NOT NULL,    -- 'utf-8', 'utf-16be', ... (matche Encoding.id)
    correct       BOOLEAN      NOT NULL,    -- agrégé : tous les steps corrects
    duration_ms   INT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attempts_user_module ON exercise_attempts(user_id, module_id);
```

#### `attempt_steps` (parent — discriminateur)
```sql
CREATE TABLE attempt_steps (
    id            BIGSERIAL PRIMARY KEY,
    attempt_id    BIGINT       NOT NULL REFERENCES exercise_attempts(id) ON DELETE CASCADE,
    position      SMALLINT     NOT NULL,
    step_type     VARCHAR(32)  NOT NULL,     -- StepType.id : 'format','binary','bit-groups',
                                             -- 'hex-bytes','code-point','endianness' (validé côté app)
    correct       BOOLEAN      NOT NULL,
    error_type    VARCHAR(64),               -- NULL si correct ; sinon clé i18n consommée par le front
    UNIQUE (attempt_id, position)
);

CREATE INDEX idx_attempt_steps_attempt ON attempt_steps(attempt_id);
```

#### Tables filles (une par `StepType`)

```sql
-- StepType.Format
CREATE TABLE attempt_step_format (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    choices      TEXT[]       NOT NULL,           -- redondant mais permet le replay/audit fidèle
    expected     VARCHAR(64)  NOT NULL,
    user_answer  VARCHAR(64)
);

-- StepType.Binary
CREATE TABLE attempt_step_binary (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     VARCHAR(64)  NOT NULL,            -- ex. "11101001"
    bit_length   SMALLINT     NOT NULL,
    user_answer  VARCHAR(64)
);

-- StepType.BitGroups
CREATE TABLE attempt_step_bit_groups (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     TEXT[]       NOT NULL,            -- ex. {"00011","101001"}
    user_answer  TEXT[]
);

-- StepType.HexBytes
CREATE TABLE attempt_step_hex_bytes (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     SMALLINT[]   NOT NULL,            -- ex. {195, 169} = {0xC3, 0xA9}
    user_answer  SMALLINT[]
);

-- StepType.CodePointEntry
CREATE TABLE attempt_step_code_point (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     INT          NOT NULL,            -- la valeur du code point (0..0x10FFFF)
    user_answer  INT
);

-- StepType.Endianness
CREATE TABLE attempt_step_endianness (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     VARCHAR(16)  NOT NULL,        -- 'BigEndian' | 'LittleEndian' (validé côté app)
    user_answer  VARCHAR(16)
);
```

#### Mapping `Step` ↔ table fille

| `StepType` enum | Table fille | Type Postgres natif des champs |
|---|---|---|
| `Format` | `attempt_step_format` | `TEXT[]`, `VARCHAR` |
| `Binary` | `attempt_step_binary` | `VARCHAR`, `SMALLINT` |
| `BitGroups` | `attempt_step_bit_groups` | `TEXT[]` |
| `HexBytes` | `attempt_step_hex_bytes` | `SMALLINT[]` |
| `CodePointEntry` | `attempt_step_code_point` | `INT` |
| `Endianness` | `attempt_step_endianness` | `VARCHAR` (enum string) |

#### Arrays Postgres natifs : choix tranché

Les step types qui portent une **séquence** (`BitGroups`, `HexBytes`) utilisent les
**arrays natifs Postgres** (`TEXT[]`, `SMALLINT[]`), pas des tables de valeurs
séparées. Justification :

- Les bits/bytes sont **toujours lus en bloc** par le validator (jamais filtrés par
  position individuelle dans le métier)
- Une table de valeurs par step augmenterait le nombre de tables à 12+
- Exposed supporte les arrays via `exposed-postgresql` (`array<Short>()`, `array<String>()`)
- Reste relationnel et typé — un `SMALLINT[]` n'est pas du JSON, il est typé,
  queryable et indexable nativement

Si un besoin d'analytics par position individuelle apparaît (ex. "à quelle position
du bit-groups l'utilisateur se trompe le plus ?"), on pourra extraire en table dédiée
à ce moment-là. Pas avant.

#### Insert d'un attempt complet

À chaque exercice soumis, le repository fait (dans une transaction Exposed) :

1. `INSERT INTO exercise_attempts ...` → récupère `id` (= `attempt_id`)
2. Pour chaque `Step` (position 0..N-1) :
   - `INSERT INTO attempt_steps (attempt_id, position, step_type, correct, error_type)`
     → récupère `step_id`
   - `INSERT INTO attempt_step_<type> (step_id, ...)` selon `step.type`

À la lecture (replay) : `JOIN` du parent avec les filles via `step_type` discriminator,
ou multi-query selon ce qui est le plus simple à exprimer en Exposed.

### Tables Spring Session JDBC
Générées via le script officiel `schema-postgresql.sql` dans une migration Flyway.

### TBD — granularity dans `module_progress` ?

À trancher au moment de la mise en place de la persistance : faut-il stocker la
progression **par granularité** (verbose/standard/compact) en plus du couple
(user_id, module_id) ? Ça permettrait des stats du genre "80% en Express, 50% en
Pas à pas". Si oui, ajouter `granularity` à l'UNIQUE de `module_progress` ou faire
une table séparée.

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
(via un intercepteur `$fetch` global Nuxt) lit ce cookie et le renvoie en header
`X-XSRF-TOKEN` sur toute requête mutante (POST/PUT/PATCH/DELETE).

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
`@nuxtjs/i18n` avec stratégie `prefix_except_default` (FR pas de préfixe, EN sous
`/en/...`). Namespaces / fichiers locaux : `common.json`, `auth.json`, `landing.json`,
`exercise.json`, `modules.json`, `feedback.json` dans `i18n/locales/{fr,en}/`.

### Synchronisation
La locale courante est dans `users.locale` (DB) ou cookie `locale` (invités). Renvoyée
dans `GET /api/auth/me`.

### Hints découplés du domaine

`domain/exercise/AnswerValidator` valide **un couple (Step, Answer) à la fois**, et
renvoie un `ValidationResult` avec `errorType` + `params` :

```kotlin
data class ValidationResult(
    val ok: Boolean,
    val errorType: String? = null,
    val params: Map<String, String> = emptyMap(),
)

class AnswerValidator {
    fun validate(step: Step, answer: Answer): ValidationResult = when (step) {
        is Step.Format         -> validateFormat(step, answer)
        is Step.Binary         -> validateBinary(step, answer)
        is Step.BitGroups      -> validateBitGroups(step, answer)
        is Step.HexBytes       -> validateHexBytes(step, answer)
        is Step.CodePointEntry -> validateCodePoint(step, answer)
        is Step.Endianness     -> validateEndianness(step, answer)
    }
    // une fonction privée par type de step
}
```

### Anti-cheat — `ValidationResult` ne porte pas la valeur attendue

**Choix tranché le 2026-05-20** : `ValidationResult` n'a **PAS** de champ `expected`,
et ses `params` ne contiennent **jamais** la valeur canonique attendue lors d'un
échec. Sinon, l'utilisateur ouvre l'onglet réseau de devtools, lit la réponse HTTP
de `/api/exercise/validate`, et obtient la solution.

Concrètement :

- `params` peuvent contenir des **hints structurels** : longueur attendue
  (`expected-length`), nombre de bytes/groupes attendus (`expected-count`),
  position d'une erreur (`position`), bornes Unicode (`min`/`max`), liste de
  choix d'un format (`choices` — déjà publique côté UI).
- `params` peuvent contenir le **`got`** (la valeur saisie par l'utilisateur) —
  c'est sa propre saisie, ce n'est pas une révélation.
- `params` ne contiennent **JAMAIS** :
  - les bits attendus pour un `Binary.wrong-value`
  - les bytes attendus pour un `HexBytes.wrong-value`
  - les groupes attendus pour un `BitGroups.wrong-value`
  - la valeur attendue pour un `CodePoint.wrong-value`
  - le bon choix pour un `Format.wrong-choice` ou `Endianness.wrong-choice`

Cas particulier : pour `Endianness` (2 choix possibles), même renvoyer le `got` de
l'utilisateur révèle la réponse par déduction. On renvoie donc juste l'`errorType`
sans aucun `params` quand le choix est faux.

La valeur attendue reste exclusivement **côté serveur** : elle vit dans le `Step`
au moment de la validation, et est persistée dans `attempt_step_<type>.expected`
pour le replay / l'audit. Elle ne traverse jamais le réseau dans une réponse
d'erreur.

### Anti-cheat — `Step.expected` à stripper au HTTP layer

**Note pour la Phase 4 (HTTP layer)** : `Step.*` carries `expected` server-side
(le validator en a besoin pour comparer à la réponse de l'utilisateur). Mais ce
champ ne doit **JAMAIS** apparaître dans la réponse JSON envoyée au front quand
le serveur retourne l'exercice initial via `POST /api/exercise/generate`.

Sinon : l'utilisateur ouvre devtools, lit `step.expected` dans la réponse, et a
la solution avant de répondre.

**Solution prévue** : custom Jackson `ValueSerializer<Step>` (sealed class → un serializer
qui dispatche sur le sous-type, ou un par sous-type) dans
`infrastructure/http/exercise/serde/` (per-feature, à côté des controllers exercise).
Le ou les serializers sont enregistrés via `@Bean exerciseJacksonModule()` dans
`ExerciseConfig`. Chaque serializer écrit uniquement les champs nécessaires au front
pour rendre l'input widget — et **omet `expected`**.

| `Step` sous-type | Champs gardés dans le JSON sortant |
|---|---|
| `Step.Binary` | `type`, `length` (pour rendre N input boxes) |
| `Step.Format` | `type`, `choices` (pour rendre les radio buttons) |
| `Step.HexBytes` | `type`, `byteCount = expected.size` (pour rendre N hex boxes) |
| `Step.BitGroups` | `type`, `groupLengths = expected.map { it.length }` (sizes des groupes) |
| `Step.CodePointEntry` | `type` (rien d'autre nécessaire) |
| `Step.Endianness` | `type` (les 2 valeurs sont connues du front) |

**Defense in depth — la DB est la vraie source de vérité** : même si le serializer
leakait `expected` par bug, la validation côté serveur reste sûre parce qu'elle
lit `expected` depuis Postgres (`attempt_step_<type>.expected` row chargée via
`attemptId`), **pas** depuis ce que l'utilisateur envoie. L'attaquant ne peut pas
manipuler la valeur attendue.

Flow complet :
1. `POST /api/exercise/generate` → server génère, persiste l'attempt + ses steps
   en DB avec `expected`, retourne l'exercise **sans** `expected`
2. `POST /api/exercise/validate { attemptId, stepIndex, answer }` → server load
   `attempt_step_<type>` row depuis DB, lit `expected`, compare à `answer`
3. Réponse `ValidationResult` (déjà sans `expected` par construction)

### Exceptions techniques vs `errorType` i18n

Deux chemins d'erreur distincts, à ne pas confondre :

| Type d'erreur | Mécanisme | Audience | Traduit ? |
|---|---|---|---|
| **Invariant de domaine violé** (`init { require(...) }`, `error()`, `IllegalArgumentException`, `IllegalStateException`) | exception qui bubble up jusqu'au global handler → HTTP 500 + log | **Dev** qui debug une stack trace | ❌ Non — message technique en anglais |
| **Validation business échouée** (réponse utilisateur incorrecte) | `ValidationResult.errorType` → HTTP 200 + body JSON | **End user** via le front | ✅ Oui — clé i18n traduite |

Exemples concrets :

```kotlin
// Invariant — un Step.Binary mal construit (bug de générateur ou de mapper DB).
// L'utilisateur ne voit JAMAIS ce message. Dev seul.
init {
    require(length > 0) { "Binary step length must be positive, got $length" }
}

// Validation — la réponse de l'user est mauvaise. L'user voit le message traduit.
return ValidationResult.incorrect(
    errorType = ErrorType.Binary.WRONG_VALUE,
    params = mapOf(ParamKey.GOT to answer.bits),
)
```

Règle : si un code path peut produire l'erreur uniquement à cause d'un **bug dans le
code**, c'est une exception technique anglais. Si l'erreur peut être produite par
une **action légitime de l'user** (saisir une mauvaise valeur), c'est un
`errorType` traduit.

### Identifiants stables — `ErrorType` et `ParamKey`, co-localisés avec leur producteur

**Choix tranché le 2026-05-20** : les identifiants stables qui voyagent vers le
front (noms d'événements métier consommés comme clés i18n, noms de variables
d'interpolation) sont déclarés comme `const val` dans des `object` situés **dans
le package du feature qui les produit**, et non dans un package transverse `i18n/`.

Pour l'instant : `domain/exercise/ErrorType.kt`, `domain/exercise/ParamKey.kt`, et
`domain/exercise/FormatChoice.kt` (identifiants des choix d'un `Step.Format`, ex.
`format-choice.byte-count.2` pour "2 bytes"). Quand `domain/auth/` produira ses
propres événements d'erreur, il aura son propre `ErrorType.kt` dans son package.
Cohérent avec l'archi feature-first.

**Distinction importante** : ces constantes ne sont **pas** des traductions. Les
traductions ("Vous avez écrit la mauvaise valeur") vivent uniquement côté front,
dans `i18n/locales/{fr,en}/feedback.json`. Les constantes Kotlin sont des
**identifiants stables d'événements métier** qui se trouvent être consommés
comme clés de translation par le front.

**Aucun string literal** au call site dans le code de production ni dans les tests :

```kotlin
// Mauvais (typo silencieux possible) :
errorType = "binary.wrong-value"
params = mapOf("expected-length" to "8")

// Bon (typo = compile error) :
errorType = ErrorType.Binary.WRONG_VALUE
params = mapOf(ParamKey.EXPECTED_LENGTH to "8")
```

Bénéfices :
- IDE autocomplete + "Rename" qui propage partout (validator, tests, futurs HTTP serializers)
- Les `ErrorType.kt` / `ParamKey.kt` de chaque feature documentent l'API stable
  produite par ce feature
- Pas de Jackson custom à écrire — `const val String` se sérialise nativement comme une string
- Les tests assertent sur les **mêmes** constantes que celles produites par le validator,
  donc renommer une clé met à jour les deux côtés en une opération

Convention de nommage : suffixe **`Type`** pour les catégories d'identifiants
(ErrorType, HintType, MessageType, ...). Pour les noms d'interpolation, c'est
**`ParamKey`** (sans suffixe Type, parce que ce ne sont pas des "types de
messages" mais des variables).

Le validator est **agnostic de l'exercice** (encoding, code point, granularity).
Il ne sait que valider un step isolé. La composition d'un exercice (quels steps,
dans quel ordre) est la responsabilité de `ExerciseGenerator` selon la `Granularity`
demandée.

Conventions de nommage des `errorType` :
- préfixe = nom du step (`binary.`, `hex-bytes.`, `format.`, ...)
- suffixe = nature de l'erreur (`wrong-value`, `too-few-bits`, `invalid-character`, ...)
- exemples : `binary.wrong-value`, `binary.too-few-bits`, `hex-bytes.wrong-byte-count`,
  `format.unknown-choice`

Le frontend traduit `errorType` via `@nuxtjs/i18n` (`$t(errorType, params)`) avec
interpolation des `params`. Les fichiers de traduction sont organisés par namespace
(`feedback.json` typiquement) avec les clés en dot-notation matchant les `errorType`.

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

JUnit 5 direct (pas Kotest spec — pour rester compatible avec les extensions Spring/Testcontainers).
Tu peux n'importer que les configs nécessaires pour gagner en vitesse :

```kotlin
@SpringBootTest(
    classes = [DatabaseConfig::class, UserConfig::class, ApplicationConfigTest::class],
)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Testcontainers
class ExposedUserRepositoryTest(
    private val userRepository: UserRepository,
) {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine")

        @JvmStatic
        @DynamicPropertySource
        @Suppress("unused")
        fun dataSourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("app.datasource.url") { postgres.jdbcUrl }
            registry.add("app.datasource.username") { postgres.username }
            registry.add("app.datasource.password") { postgres.password }
            registry.add("app.datasource.driver-class-name") { postgres.driverClassName }
        }
    }

    @Test
    fun `create persists and returns the generated id`() { ... }
}
```

Points-clés :

- `ApplicationConfigTest` (en `src/intTest/.../config/`) override `@Bean clock` avec une
  `Instant` fixe pour rendre les timestamps déterministes
- `spring.main.allow-bean-definition-overriding=true` dans `src/intTest/resources/application.yaml`
  pour autoriser cet override
- Les controller tests utilisent `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(ApplicationConfigTest::class)`
- `flyway-database-postgresql` est requis en `runtimeOnly` (Flyway 11.x a sorti le support
  Postgres de son module core)

### Tests de serializers (purs, sans Spring)
Voir section "Sérialisation JSON" ci-dessus.

### Couverture minimale obligatoire
- `domain/encoding/Codec` : tous les cas frontières (U+007F, U+0080, U+07FF, U+0800,
  U+FFFF, U+10000, U+10FFFF, surrogates UTF-16) — pour `encode()` ET `decode()`
- `domain/exercise/AnswerValidator` : chaque `errorType` produit avec les bons `params`
- `infrastructure/repository/*` : upsert, find, contraintes uniques
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

    # SSR Nuxt (Node)
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
- **Node 22+** pour Nuxt SSR
- **Docker Compose** uniquement pour Postgres 18 (isolation simple)
- **systemd** units pour Spring Boot et Nuxt

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
2. `domain/encoding/` : `CodePoint`, `Encoding`, `Codec` (encode/decode pour les
   8 encodings), `EncoderException`, `DecoderException`, `ByteArrayExt`. Tests Kotest
   complets (frontières UTF-8/16/32, surrogates, BOM, overlong, sign-extension).
3. `domain/exercise/` : `Granularity`, `StepType`, `Step` sealed class, `Answer` sealed
   class, `ValidationResult`, `Exercise`, `ExerciseAttempt`, `ExerciseAttemptRepository`
   (interface), `AnswerValidator`, `ExerciseGenerator`. Tests par type de step.
4. `domain/progress/`, `domain/user/` : entités + repositories (interfaces) + services
   au besoin. Pour les timestamps, on injecte `kotlin.time.Clock` (stdlib) — pas
   d'interface custom.
5. **Pas de `config/DomainConfig` central** — les services domain seront wirés au moment de
   leur premier consommateur, dans le `<Feature>Config` correspondant. Ex. `ExerciseGenerator`,
   `AnswerValidator`, `Codec` iront dans `ExerciseConfig` quand la Phase 5 (Exercise API)
   arrivera. Idem `ProgressService` dans `ProgressConfig`.

### Phase 2 — Infrastructure DB ✅
6. Postgres + Flyway (autoconfig), migrations `users`, `module_progress`,
   `exercise_attempts`, tables Spring Session
7. `infrastructure/repository/<feature>/` : tables Exposed + repositories (classes Kotlin pures)
8. `config/DatabaseConfig` (`DataSource` + `Database` Exposed) + un `<Feature>Config`
   par entité regroupant repo + JacksonModule (ex. `UserConfig` avec `@Bean userRepository`
   + `@Bean userJacksonModule`)
9. Tests d'intégration Testcontainers ciblés (`classes = [DatabaseConfig::class, UserConfig::class, ApplicationConfigTest::class]`)

### Phase 3 — Sécurité + Auth fundamentals
10. `config/SecurityConfig` : `SecurityFilterChain` JSON-friendly (CSRF cookie X-XSRF-TOKEN,
    formLogin/httpBasic disabled, logout custom 204, 401 entry point), `PasswordEncoder`
    (BCrypt strength 12), `AuthenticationManager`, `SecurityContextRepository` (HttpSession-based),
    `PersistentTokenRepository` (JDBC) + `RememberMeServices` (persistent-token, 2 semaines)
11. `infrastructure/security/` : `CustomUserDetailsService` basé sur `UserRepository.findByEmail`,
    `UserDetailsAdapter` minimal (userId + email + passwordHashValue — pas le User complet,
    pour ne pas forcer `User` à implémenter `Serializable`), `BCryptPasswordHasher` (impl du
    port domain `PasswordHasher`)
12. `domain/auth/` : `AuthService.register(...)`, port `PasswordHasher`, `AuthErrorType`
    (`EMAIL_ALREADY_TAKEN`, `BAD_CREDENTIALS`, `SESSION_ORPHANED`), `OrphanedSessionException`
13. `infrastructure/http/auth/` : DTOs `RegisterRequest`/`LoginRequest`, controllers
    `POST /api/auth/{register,login,logout}` + `GET /api/auth/me`. Login en JSON (pas form),
    contrôle manuel de l'`AuthenticationManager` + sauvegarde SecurityContext + remember-me
    via wrapper `HttpServletRequest`
14. `infrastructure/http/auth/serde/UserSerializer.kt` (Jackson 3 `ValueSerializer<User>`,
    omet `passwordHash` et `updatedAt`)
15. `infrastructure/http/GlobalExceptionHandler` (`@RestControllerAdvice`) :
    `EmailAlreadyTakenException` → 409, `BadCredentialsException` → 401,
    `OrphanedSessionException` → 401 + session invalidée, `MethodArgumentNotValidException`
    → **422** (validation, pas 400)
16. Spring Session JDBC activé via `@EnableJdbcHttpSession` + Flyway migration des tables
    `spring_session*` + `persistent_logins` (remember-me)
17. Tests d'intégration auth (register OK/conflict/422, login OK/wrong-creds/remember-me,
    /me auth/non-auth, logout invalide la session) + unit test `UserSerializerTest`

### Phase 4 — Reset password + profile management
18. Migration `password_reset_tokens` + service de génération/expiration de token + envoi
    email (Mailpit en dev)
19. Controllers `POST /api/auth/{forgot-password,reset-password}`
20. Controllers `PATCH /api/profile`, `PATCH /api/profile/password`, `DELETE /api/profile`
21. Gestion `updatedAt` dans `UserSerializer` (`writeNullProperty` quand null)

### Phase 5 — Exercise + Progress backend API
22. Controllers `/api/exercise/generate` et `/api/exercise/validate`, `/api/progress`
23. `infrastructure/http/exercise/serde/StepSerializer` (anti-cheat : omet `expected`)
    enregistré via `exerciseJacksonModule` dans `ExerciseConfig`
24. Tests d'intégration end-to-end exercice

### Phase 6 — Frontend setup
25. `npx nuxi@latest init web` (template `ui`) → Nuxt 4 + Nuxt UI v4 + Tailwind v4 + TypeScript strict
26. Intercepteur `$fetch` global (credentials, CSRF header, parsing zod des responses)
27. `@nuxtjs/i18n` setup avec namespaces FR/EN, FR par défaut
28. Layouts `auth.vue` et `default.vue` dans `layouts/`, header/footer globaux

### Phase 7 — Auth UI
29. Pages `login.vue`, `register.vue`, `forgot-password.vue`, `reset-password.vue`
    (VeeValidate + zod + Nuxt UI)
30. Page `profile.vue` avec sections (compte, langue, password, danger zone)

### Phase 8 — Exercises UI
31. Composants atomiques : `BitDisplay`, `BitInput`, `HexInput`, `ByteDisplay`,
    `FormatSelector`, `EndiannessSelector`, `StepProgress`, `FeedbackPanel`
32. Module **Encoder en UTF-8** complet (niveaux 1 à 5) — le module phare
33. Module **Décoder UTF-8**
34. Modules UTF-16 encode/decode
35. Modules UTF-32 encode/decode
36. Module Endianness
37. Module Mojibake
38. Module Identifier l'encodage

### Phase 9 — Landing + polish
39. Landing mode invité (hero + CTAs)
40. Landing mode connecté (cartes modules + progression)
41. Random total
42. Thèmes light/dark finalisés
43. Responsive mobile complet
44. Audit accessibilité (clavier, lecteur d'écran)

### Phase 10 — Déploiement
45. Dockerfile Spring Boot
46. Build Nuxt en mode SSR Node (`nuxt build` → `.output/server/index.mjs`)
47. Caddyfile production
48. Migrations en CI/CD
49. Monitoring basique (Spring Boot Actuator + logs)

### À envisager plus tard (post-v1)
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
- Pas de Server Components (on reste en pattern Nuxt classique SSR + client hydration)
- Pas de cache Redis tant que pas nécessaire
- Pas de `@ControllerAdvice` ni OpenAPI tant que le squelette n'est pas en place
