# AGENTS.md — Charset Playground (réécriture Phoenix/LiveView)

## Contexte : cette branche est une migration

Cette branche (`phoenix`) est une **réécriture complète** de charset.school, repartie de
zéro. Elle deviendra le nouveau `main` à la fin de la migration.

- **L'ancienne implémentation vit sur la branche `main`** : Kotlin 2.x + Spring Boot 4
  (backend REST) + Nuxt 4 / Vue 3 (frontend SSR). ~18 000 lignes au total.
- **La cible** : un monolithe Phoenix/LiveView unique. Plus de séparation front/back,
  plus d'API REST, plus de couche de sérialisation.
- **Le métier et le design ne changent pas.** On porte à l'identique : la logique
  d'encodage/décodage, les générateurs d'exercices, la validation step-by-step, le modèle
  de données, les textes i18n FR/EN, et le design Tailwind existant (classes reprises
  quasi verbatim depuis les templates Vue).

### Workflow git pendant la migration

**`phoenix` est la branche d'intégration, pas `main`.** Tant que la migration n'est pas
terminée :

- Toute nouvelle branche de travail part de `phoenix` (`git switch -c <branche> phoenix`),
  jamais de `main`.
- Les PRs ciblent `phoenix` comme branche de base, jamais `main`
  (`gh pr create --base phoenix`).
- `main` reste figée en lecture seule : c'est la référence de l'ancienne implémentation
  (métier, design, schéma, i18n). On n'y committe plus rien.
- À la fin de la migration (Phase 7), `phoenix` deviendra le nouveau `main`.

### Consulter l'ancien code (référence permanente pendant la migration)

```bash
git show main:src/main/kotlin/school/charset/app/domain/encoding/Codec.kt
git show main:web/app/pages/sandbox/encode/utf-8.vue
git show main:CLAUDE.md                          # conventions détaillées de l'ancien projet
git ls-tree -r main --name-only | grep sandbox   # lister les fichiers d'une zone
```

Zones clés sur `main` :

| Zone | Chemin sur `main` |
|---|---|
| Domaine encodage (Codec, Windows1252Spec, CodePoint, Encoding) | `src/main/kotlin/school/charset/app/domain/encoding/` |
| Domaine exercice (Step, Answer, AnswerValidator, ErrorType, ParamKey, generators) | `src/main/kotlin/school/charset/app/domain/exercise/` |
| Domaine sandbox (parsers, SandboxService) | `src/main/kotlin/school/charset/app/domain/sandbox/` |
| Domaine progression | `src/main/kotlin/school/charset/app/domain/progress/` |
| Tests unitaires (la spec exécutable du domaine) | `src/test/kotlin/` |
| Tests d'intégration (contrats HTTP, repos) | `src/intTest/kotlin/` |
| Migrations SQL (schéma à reprendre tel quel) | `src/main/resources/db/migration/` |
| Pages et composants Vue (design de référence) | `web/app/pages/`, `web/app/components/` |
| Locales FR/EN | `web/i18n/locales/{fr,en}/` |
| Theme / tokens Tailwind | `web/app/` (`app.config.ts`, CSS), `theme/` |

## Produit (rappel)

Exercices interactifs pour apprendre **l'encodage et le décodage** des caractères :
ASCII, Latin-1, Windows-1252, UTF-8, UTF-16, UTF-32, endianness, BOM.

- **Sandbox** : 10 pages (encode/decode × utf-8, utf-16, utf-32, latin1, windows-1252)
  où l'utilisateur saisit un caractère/des bytes et voit la conversion décomposée étape
  par étape, avec feedback immédiat à la frappe.
- **Exercices** : l'utilisateur fait les conversions à la main, step par step, avec
  validation immédiate côté serveur, hints gradués (3 niveaux), et progression persistée.
- **Pas de compte utilisateur** : tout est keyé par un token anonyme opaque dans un
  cookie HttpOnly (l'auth a été retirée de l'ancienne version, ne pas la réintroduire).

## Stack cible

| Brique | Choix | Version |
|---|---|---|
| Langage | Elixir | **1.20** (OTP 29) |
| Framework | Phoenix | **1.8.7** |
| UI temps réel | Phoenix LiveView | **1.1.30** |
| HTTP server | Bandit | dernière compatible |
| DB | SQLite (mode WAL) | via `ecto_sqlite3` |
| Backups | Litestream (réplication continue du WAL vers un stockage objet) | — |
| ORM / migrations | Ecto + ecto_sql + ecto_sqlite3 | dernière compatible |
| CSS | Tailwind CSS v4 **via Vite** | tailwind 4.x + vite |
| i18n | Gettext (EN par défaut, FR sous `/fr`) | `{:gettext, "~> 1.0"}` |
| Assets pipeline | **Vite** (pas esbuild, pas tailwind CLI) — il y a donc un `package.json` | — |
| Conteneurisation | Docker multi-stage (voir section Docker) | — |

### Pourquoi Elixir pour ce projet

Le domaine métier est littéralement « découper des code points en bits ». Les bitstrings
Elixir expriment les formats d'encodage directement dans la syntaxe :

```elixir
# UTF-8 2 bytes : le format EST le pattern
<<0b110::3, high::5, 0b10::2, low::6>>

# Décodage par pattern matching déclaratif
def decode_utf8(<<0::1, cp::7, rest::binary>>), do: {cp, rest}
def decode_utf8(<<0b110::3, h::5, 0b10::2, l::6, rest::binary>>), do: {Bitwise.bsl(h, 6) + l, rest}
```

Le port du `Codec` Kotlin (masques/shifts manuels) doit exploiter ce style — c'est la
raison d'être de la migration, pas un détail.

### Typage — exploiter le type system d'Elixir 1.20 au maximum

Elixir 1.20 marque le premier jalon production-ready du type system set-theoretic :
**inférence automatique sur tout le code, sans annotation** (voir
https://elixir-lang.org/blog/2026/06/03/elixir-v1-20-0-released/). Le compilateur
infère les types depuis les guards, le pattern matching et le control flow, et
remonte les violations garanties d'échouer au runtime.

Important : les **signatures de types écrites par le développeur n'existent pas encore
en 1.20** (elles viendront avec les typed structs puis les signatures dans les versions
suivantes). « Utiliser les types au maximum » signifie donc écrire du code que
l'inférence peut exploiter :

- **Toutes les violations de typage sont des erreurs.** `mix compile
  --warnings-as-errors` dans `mix precommit` et en CI — un warning de typage ne se
  merge pas.
- **Guards systématiques** sur les fonctions publiques du domaine (`when is_integer(cp)
  and cp >= 0 and cp <= 0x10FFFF`) : c'est à la fois la validation des invariants
  (l'équivalent des `init { require(...) }` Kotlin de main) et de l'information de type
  pour l'inférence.
- **Structs partout, maps nues nulle part** dans le domaine : `%Step.Binary{}`,
  `%ValidationResult{}`, etc. avec `@enforce_keys` sur les champs obligatoires.
  L'inférence narrowe les structs bien mieux que les maps anonymes.
- **Pattern matching plutôt qu'accès dynamique** : `%Step.Binary{expected: expected} =
  step` plutôt que `step.expected` sur un type incertain, `case`/`with` avec clauses
  exhaustives plutôt que conditions sur des valeurs non narrowées.
- **Pas de `dynamic()` volontaire** : éviter les constructions qui forcent le
  compilateur à abandonner le narrowing (maps hétérogènes fourre-tout, `apply/3`,
  `Map.get` sur des structs).
- **`@spec` sur l'API publique des contexts et du domaine** : pas encore consommées par
  le nouveau type system, mais elles documentent, alimentent Dialyzer si on l'ajoute,
  et préparent la conversion vers les vraies signatures quand elles arriveront dans une
  future version — on les écrira alors en priorité sur le domaine.
- Quand une nouvelle version d'Elixir étend le type system (typed structs, signatures),
  **adopter immédiatement** sur `Charset.Encoding` et `Charset.Exercise` — le domaine
  est petit, pur et déjà entièrement testé, c'est le candidat idéal.

## Architecture cible

Phoenix Contexts classiques. Le domaine reste pur (pas d'Ecto, pas de Phoenix dans les
modules de calcul), même esprit que l'archi ports & adapters de l'ancien projet.

**Namespaces (choix tranché le 2026-06-05)** : l'OTP app s'appelle **`:app`** (style
tracker-tv). `App`/`AppWeb` portent l'infrastructure (Application, Repo, Mailer,
endpoint, LiveViews) ; les **domaines** sont des namespaces top-level à côté -
`Charset.*` aujourd'hui (encoding, exercise, ...), d'autres demain (ex. `Binary.*`)
sans toucher à la couche web.

```
lib/
  charset/                          # Domaine charset (pur, pas d'Ecto ni Phoenix)
    encoding/                       # Codec, specs d'encodage
      codec.ex                      # encode/decode pour les 8 encodings (bitstrings !)
      code_point.ex
      windows1252.ex                # table de mapping 0x80..0x9F
    exercise/                       # Pur : steps, answers, validation, génération
      step.ex                       # structs par type de step (format, binary, bit_groups, hex_bytes, code_point, endianness)
      answer.ex
      answer_validator.ex           # validate(step, answer) -> %ValidationResult{}
      validation_result.ex          # ok / error_type / params / hint_level — JAMAIS de `expected`
      error_type.ex                 # identifiants stables (clés gettext côté UI)
      param_key.ex
      generator/                    # un générateur par encoding × niveau
    sandbox/                        # Pur : parsers d'input + décomposition step-by-step
    progress/                       # Context Ecto : module_progress (token, module_id)
    exercise_attempts.ex            # Context Ecto : attempts + steps (table-per-type)
    schema/                         # Schemas Ecto (un fichier par table)
  app/                              # Infrastructure app-wide (App.*)
    application.ex                  # OTP supervision tree
    repo.ex                         # App.Repo (SQLite)
    mailer.ex
  app_web/                          # Couche web partagée par tous les domaines (AppWeb.*)
    router.ex
    plugs/visitor_token.ex          # pose/lit le cookie token anonyme HttpOnly
    live/
      sandbox/                      # 10 LiveViews (encode/decode × 5 encodings)
      exercise_live.ex              # flow d'exercice step-by-step
    controllers/page_controller.ex  # landing (statique, SEO)
    components/                     # core_components + composants portés depuis Vue
assets/                             # Vite + Tailwind v4 (package.json à la racine du repo)
priv/repo/migrations/
priv/gettext/{fr,en}/LC_MESSAGES/
test/                               # Miroir de lib/ — le port des tests Kotest/JUnit de main
```

### Ce qui disparaît par rapport à main (et ne doit PAS être recréé)

- L'API REST (`/api/exercise/*`, `/api/progress`, `/api/sandbox/*`) — LiveView fait le
  round-trip via websocket.
- Les DTOs, serializers Jackson custom, et toute la logique « stripper `expected` au
  HTTP layer » — voir anti-cheat ci-dessous.
- Le client API front (`useFetch`, intercepteur XSRF, zod, types TS partagés).
- Le double runtime Node SSR + JVM : un seul release OTP derrière Caddy.

### Anti-cheat — par construction avec LiveView

Règle invariante héritée de main : **la valeur attendue (`expected`) ne traverse jamais
le réseau vers le client.**

En LiveView c'est structurel : `expected` vit dans les assigns du process serveur (et en
DB dans `attempt_step_<type>.expected`). Il n'y a pas de couche JSON où il pourrait
leaker. Vigilance quand même :

- Ne jamais interpoler `expected` dans le HEEx (même dans un attribut `data-*` ou un
  commentaire HTML).
- `ValidationResult.params` peut contenir des hints structurels (`expected_length`,
  `expected_count`, `position`, bornes) et le `got` de l'utilisateur — **jamais** la
  valeur canonique attendue. Cas particulier `endianness` : 2 choix possibles, donc même
  le `got` révèle la réponse — aucun params.
- La DB reste la source de vérité pour la validation (defense in depth) : on lit
  `expected` depuis la row `attempt_step_<type>`, jamais depuis ce qu'envoie le client.

### Hints gradués (repris de main)

1ère erreur → `hint_level: 1` (question ouverte), 2ème → `2` (indice conceptuel),
3ème+ → niveau `3` (réponse + raison) **uniquement sur demande explicite** (bouton
« Donne-moi la réponse »). Le compteur `attempts` et le flag `revealed` vivent dans
`attempt_steps`. Clé i18n côté UI : `feedback.{error_type}.level{hint_level}`.

## Modèle de données

**Reprendre le schéma de `main`** (mêmes tables, mêmes colonnes, mêmes index), converti
en migrations Ecto pour SQLite. Source : `git show main:src/main/resources/db/migration/...`
L'ancien schéma était Postgres ; la seule adaptation est le mapping des arrays (voir
ci-dessous).

- `exercise_attempts` (token, module_id, level, code_point, encoding, correct,
  finalized, duration_ms) + index `(token, module_id)`
- `attempt_steps` (attempt_id, position, step_type, correct, error_type, attempts,
  revealed) + UNIQUE(attempt_id, position)
- 6 tables filles table-per-type : `attempt_step_format`, `_binary`, `_bit_groups`,
  `_hex_bytes`, `_code_point`, `_endianness`
- `module_progress` (token, module_id, level, streak, attempts, errors,
  last_played_at) + UNIQUE(token, module_id)

Conventions DB conservées :
- **Pas de CHECK constraints sur les valeurs énumérées** (l'app est le seul writer, les
  enums Elixir/Ecto sont la source de vérité). Les FK restent (`PRAGMA foreign_keys=ON`,
  activé par `ecto_sqlite3`).
- Les séquences de bits/bytes (`bit_groups.expected`, `hex_bytes.expected`, ...) étaient
  des arrays Postgres natifs (`TEXT[]`, `SMALLINT[]`) sur main. En SQLite : champs Ecto
  `{:array, :string}` / `{:array, :integer}`, stockés en JSON text par `ecto_sqlite3`.
  Le domaine ne query jamais ces séquences par position (toujours lues en bloc par le
  validator), donc la perte de queryabilité est théorique. Pas de table de valeurs.
- Timestamps UTC.

### Réglages SQLite

- **Mode WAL obligatoire** (`journal_mode: :wal` dans la config `ecto_sqlite3`) :
  lecteurs et writer ne se bloquent pas, indispensable pour un serveur web.
- `busy_timeout` raisonnable (ex. 5000 ms) pour absorber la contention single-writer.
- Le fichier DB vit dans un répertoire dédié monté en volume Docker (ex. `/data/charset.db`),
  configuré via `DATABASE_PATH` dans `config/runtime.exs`.

## i18n

- **Gettext**, **EN locale par défaut** (msgids = texte anglais), FR en second sous le
  préfixe `/fr/...` (équivalent de la stratégie `prefix_except_default` de l'ancien
  front, qui avait `defaultLocale: 'en'` malgré ce que disait son CLAUDE.md).
- Mécanique en place : plug `AppWeb.Plugs.Locale` (assigns `:locale` +
  `:alternate_path`), scopes router dupliqués `/` et `/fr`, helper
  `localized_path/2`, traductions dans `priv/gettext/fr/LC_MESSAGES/`.
- Porter les locales depuis `main:web/i18n/locales/{fr,en}.json` (~1 100 lignes) au
  fil des pages.
- Les `error_type` produits par le domaine (`binary.wrong-value`, etc.) sont des
  **identifiants stables** déclarés comme constantes dans `error_type.ex` — jamais de
  string literal au call site (ni en prod ni en test). L'UI les mappe vers des clés
  gettext.

### Conventions de contenu user-facing (NON négociables, héritées de main)

- **Tutoiement en FR**, toujours (« Crée ton compte », « Saisis ton mot de passe »).
- **Tiret simple `-`, jamais d'em dash `—`** dans tout contenu visible par
  l'utilisateur (templates HEEx, fichiers gettext). Les commentaires de code ne sont
  pas concernés.
- **Les commentaires de code sont en anglais**, même si la doc projet est en français.

## Design / front

- Le design de référence est celui de `main` : reprendre les classes Tailwind des
  templates Vue quasi verbatim dans les HEEx. Comparer visuellement avec le site
  existant (lancer les deux côte à côte) avant de considérer une page terminée.
- Les composants Nuxt UI (boutons, tooltips, toggles, menus) n'ont pas d'équivalent :
  les recréer en HEEx + Tailwind dans `app_web/components/`.
- Composants d'exercice à porter (depuis `main:web/app/components/exercise/`) :
  BitInput, BitGroupsInput, HexInput, CodePointInput, FormatSelector, OffsetInput,
  UsefulBitCountInput, StepProgress, FeedbackPanel, BitDisplay, ExerciseSubHeader.
- Sandbox : feedback à la frappe via `phx-change` + debounce (l'ancien front appelait
  l'API à chaque frappe ; ici l'état du LiveView fait le travail).
- Conserver les meta OG/Twitter et l'image de preview sociale (voir commits récents de
  `main` : OG image light theme, logo, JetBrains Mono).
- Dark/light theme toggle et locale toggle existent sur main : à conserver.

## Assets — Vite + Tailwind v4

Contrairement au scaffolding Phoenix par défaut (esbuild + tailwind CLI), ce projet
utilise **Vite** :

- `package.json` avec `vite`, `tailwindcss` 4.x, `@tailwindcss/vite`.
- Build de prod : Vite émet dans `priv/static/assets/`, puis `mix phx.digest`.
- Dev : watcher Vite déclaré dans `config/dev.exs` (`watchers: [npm: ["run", "dev"]]`
  ou équivalent), HMR pour le CSS/JS.
- Tailwind v4 : config par CSS (`@theme` dans `assets/css/app.css`), pas de
  `tailwind.config.js`. Porter les tokens du theme depuis main.

## Docker

S'inspirer de `/Users/florent/Code/tracker-tv/tracker-tv/Dockerfile` (projet Elixir du
même auteur), en adaptant le stage assets à Vite :

1. **Stage assets** : `node:24-alpine` → `npm ci` → `npm run build` (Vite) →
   `priv/static/assets/`
2. **Stage builder** : `elixir:1.20-otp-29-alpine` → `mix deps.get --only prod` →
   compile → `mix phx.digest` → `mix release`
3. **Stage runtime** : `alpine` nu + `libstdc++ openssl ncurses-libs ca-certificates`,
   user non-root, `ENV LANG=C.UTF-8`, `EXPOSE 4000`, `CMD ["bin/app", "start"]`

Runtime config via env vars dans `config/runtime.exs` : `DATABASE_PATH` (fichier SQLite
sur un volume), `SECRET_KEY_BASE`, `PHX_HOST`, `PORT`. Caddy reste le reverse proxy
devant (un seul backend désormais, plus de split :3000/:8080).

Pas de service DB dans le compose : SQLite est embarqué, le fichier vit sur un volume.
**Backups via Litestream** : réplication continue du WAL vers un stockage objet
(R2/B2/S3), soit en sidecar dans le compose, soit en superviseur du release
(`litestream replicate -exec "bin/app start"`). Restauration point-in-time via
`litestream restore`.

## Tests

- **ExUnit** partout. Les tests Kotest/JUnit de `main` (~6 200 lignes) sont la spec du
  domaine : les porter AVANT ou AVEC le code qu'ils couvrent, pas après.
- **Doctests** bienvenus sur le codec (les exemples d'encodage font une excellente doc).
- Couverture minimale obligatoire (héritée de main) :
  - `Charset.Encoding.Codec` : tous les cas frontières — U+007F, U+0080, U+07FF,
    U+0800, U+FFFF, U+10000, U+10FFFF, surrogates UTF-16 — pour encode ET decode,
    chaque encoding, chaque endianness.
  - `AnswerValidator` : chaque `error_type` produit avec les bons `params`.
  - Contexts Ecto : upsert, find, contraintes uniques (via la DB de test, pas de mocks).
  - LiveViews : tests `Phoenix.LiveViewTest` sur les flows sandbox et exercice,
    incluant « `expected` n'apparaît jamais dans le HTML rendu ».
- Sandbox de test DB : `Ecto.Adapters.SQL.Sandbox` sur SQLite (fichier de test dédié,
  pas de service externe à démarrer). Contrainte single-writer : les tests qui touchent
  la DB restent en `async: false` ; les tests du domaine pur (la grande majorité)
  restent async.

## Plan de migration (phases)

Cocher au fil de l'eau. Chaque phase doit laisser la branche verte (`mix test` passe).

- [ ] **Phase 0 — Squelette** : `mix phx.new`, Vite + Tailwind v4 câblés, Gettext
      FR/EN, plug visitor token, Dockerfile + compose, CI minimale. `mix phx.server`
      affiche une page.
- [ ] **Phase 1 — Domaine encoding** : port de `Codec` + `Windows1252Spec` +
      `CodePoint` + `Encoding` en bitstrings, avec TOUS les tests frontières portés.
- [x] **Phase 2 — Domaine exercise** : steps, answers, `AnswerValidator` (chaque
      `error_type` testé), generators par encoding × niveau. (Les hints gradués
      restent côté Phase 5 : le compteur `attempts` vit dans la persistance.)
- [ ] **Phase 3 — Persistance** : migrations Ecto (schéma de main, arrays adaptés en
      JSON), schemas, contexts attempts + progress, tests d'intégration DB.
- [x] **Phase 4 — Sandbox LiveView** : les 10 pages, parsers portés, feedback à la
      frappe, design comparé visuellement à main.
- [ ] **Phase 5 — Exercice LiveView** : flow step-by-step complet (génération,
      validation, hints, reveal, finalisation, progression).
- [ ] **Phase 6 — Landing + i18n + SEO** : page d'accueil, locales complètes FR/EN,
      meta OG/Twitter + image sociale, sitemap/robots.
- [ ] **Phase 7 — Parité & bascule** : checklist de parité fonctionnelle page par page
      contre main, déploiement Docker, puis cette branche devient le nouveau `main`.

## Commandes

```bash
mix setup            # deps + DB + npm install (alias à définir dans mix.exs)
mix phx.server       # dev server (lance le watcher Vite)
mix test             # tests
mix precommit        # alias : compile --warnings-as-errors + format --check + test
npm run build        # build Vite de prod (depuis assets/ ou la racine selon le setup)
```

---

# Guidelines Phoenix/Elixir (générées par phx.new)

This is a web application written using the Phoenix web framework.

## Project guidelines

- Use `mix precommit` alias when you are done with all changes and fix any pending issues
- Use the already included and available `:req` (`Req`) library for HTTP requests, **avoid** `:httpoison`, `:tesla`, and `:httpc`. Req is included by default and is the preferred HTTP client for Phoenix apps

### Phoenix v1.8 guidelines

- **Always** begin your LiveView templates with `<Layouts.app flash={@flash} ...>` which wraps all inner content
- The `MyAppWeb.Layouts` module is aliased in the `my_app_web.ex` file, so you can use it without needing to alias it again
- Anytime you run into errors with no `current_scope` assign:
  - You failed to follow the Authenticated Routes guidelines, or you failed to pass `current_scope` to `<Layouts.app>`
  - **Always** fix the `current_scope` error by moving your routes to the proper `live_session` and ensure you pass `current_scope` as needed
- Phoenix v1.8 moved the `<.flash_group>` component to the `Layouts` module. You are **forbidden** from calling `<.flash_group>` outside of the `layouts.ex` module
- Out of the box, `core_components.ex` imports an `<.icon name="hero-x-mark" class="w-5 h-5"/>` component for hero icons. **Always** use the `<.icon>` component for icons, **never** use `Heroicons` modules or similar
- **Always** use the imported `<.input>` component for form inputs from `core_components.ex` when available. `<.input>` is imported and using it will save steps and prevent errors
- If you override the default input classes (`<.input class="myclass px-2 py-1 rounded-lg">)`) class with your own values, no default classes are inherited, so your
custom classes must fully style the input

### JS and CSS guidelines

- **Use Tailwind CSS classes and custom CSS rules** to create polished, responsive, and visually stunning interfaces.
- Tailwindcss v4 **no longer needs a tailwind.config.js** and uses a new import syntax in `app.css`:

      @import "tailwindcss" source(none);
      @source "../css";
      @source "../js";
      @source "../../lib/my_app_web";

- **Always use and maintain this import syntax** in the app.css file for projects generated with `phx.new`
- **Never** use `@apply` when writing raw css
- **Always** manually write your own tailwind-based components instead of using daisyUI for a unique, world-class design
- Out of the box **only the app.js and app.css bundles are supported**
  - You cannot reference an external vendor'd script `src` or link `href` in the layouts
  - You must import the vendor deps into app.js and app.css to use them
  - **Never write inline <script>custom js</script> tags within templates**

### UI/UX & design guidelines

- **Produce world-class UI designs** with a focus on usability, aesthetics, and modern design principles
- Implement **subtle micro-interactions** (e.g., button hover effects, and smooth transitions)
- Ensure **clean typography, spacing, and layout balance** for a refined, premium look
- Focus on **delightful details** like hover effects, loading states, and smooth page transitions


<!-- usage-rules-start -->

<!-- phoenix:elixir-start -->
## Elixir guidelines

- Elixir lists **do not support index based access via the access syntax**

  **Never do this (invalid)**:

      i = 0
      mylist = ["blue", "green"]
      mylist[i]

  Instead, **always** use `Enum.at`, pattern matching, or `List` for index based list access, ie:

      i = 0
      mylist = ["blue", "green"]
      Enum.at(mylist, i)

- Elixir variables are immutable, but can be rebound, so for block expressions like `if`, `case`, `cond`, etc
  you *must* bind the result of the expression to a variable if you want to use it and you CANNOT rebind the result inside the expression, ie:

      # INVALID: we are rebinding inside the `if` and the result never gets assigned
      if connected?(socket) do
        socket = assign(socket, :val, val)
      end

      # VALID: we rebind the result of the `if` to a new variable
      socket =
        if connected?(socket) do
          assign(socket, :val, val)
        end

- **Never** nest multiple modules in the same file as it can cause cyclic dependencies and compilation errors
- **Never** use map access syntax (`changeset[:field]`) on structs as they do not implement the Access behaviour by default. For regular structs, you **must** access the fields directly, such as `my_struct.field` or use higher level APIs that are available on the struct if they exist, `Ecto.Changeset.get_field/2` for changesets
- Elixir's standard library has everything necessary for date and time manipulation. Familiarize yourself with the common `Time`, `Date`, `DateTime`, and `Calendar` interfaces by accessing their documentation as necessary. **Never** install additional dependencies unless asked or for date/time parsing (which you can use the `date_time_parser` package)
- Don't use `String.to_atom/1` on user input (memory leak risk)
- Predicate function names should not start with `is_` and should end in a question mark. Names like `is_thing` should be reserved for guards
- Elixir's builtin OTP primitives like `DynamicSupervisor` and `Registry`, require names in the child spec, such as `{DynamicSupervisor, name: MyApp.MyDynamicSup}`, then you can use `DynamicSupervisor.start_child(MyApp.MyDynamicSup, child_spec)`
- Use `Task.async_stream(collection, callback, options)` for concurrent enumeration with back-pressure. The majority of times you will want to pass `timeout: :infinity` as option

## Mix guidelines

- Read the docs and options before using tasks (by using `mix help task_name`)
- To debug test failures, run tests in a specific file with `mix test test/my_test.exs` or run all previously failed tests with `mix test --failed`
- `mix deps.clean --all` is **almost never needed**. **Avoid** using it unless you have good reason

## Test guidelines

- **Always use `start_supervised!/1`** to start processes in tests as it guarantees cleanup between tests
- **Avoid** `Process.sleep/1` and `Process.alive?/1` in tests
  - Instead of sleeping to wait for a process to finish, **always** use `Process.monitor/1` and assert on the DOWN message:

      ref = Process.monitor(pid)
      assert_receive {:DOWN, ^ref, :process, ^pid, :normal}

   - Instead of sleeping to synchronize before the next call, **always** use `_ = :sys.get_state/1` to ensure the process has handled prior messages
<!-- phoenix:elixir-end -->

<!-- phoenix:phoenix-start -->
## Phoenix guidelines

- Remember Phoenix router `scope` blocks include an optional alias which is prefixed for all routes within the scope. **Always** be mindful of this when creating routes within a scope to avoid duplicate module prefixes.

- You **never** need to create your own `alias` for route definitions! The `scope` provides the alias, ie:

      scope "/admin", AppWeb.Admin do
        pipe_through :browser

        live "/users", UserLive, :index
      end

  the UserLive route would point to the `AppWeb.Admin.UserLive` module

- `Phoenix.View` no longer is needed or included with Phoenix, don't use it
<!-- phoenix:phoenix-end -->

<!-- phoenix:ecto-start -->
## Ecto Guidelines

- **Always** preload Ecto associations in queries when they'll be accessed in templates, ie a message that needs to reference the `message.user.email`
- Remember `import Ecto.Query` and other supporting modules when you write `seeds.exs`
- `Ecto.Schema` fields always use the `:string` type, even for `:text`, columns, ie: `field :name, :string`
- `Ecto.Changeset.validate_number/2` **DOES NOT SUPPORT the `:allow_nil` option**. By default, Ecto validations only run if a change for the given field exists and the change value is not nil, so such as option is never needed
- You **must** use `Ecto.Changeset.get_field(changeset, :field)` to access changeset fields
- Fields which are set programmatically, such as `user_id`, must not be listed in `cast` calls or similar for security purposes. Instead they must be explicitly set when creating the struct
- **Always** invoke `mix ecto.gen.migration migration_name_using_underscores` when generating migration files, so the correct timestamp and conventions are applied
<!-- phoenix:ecto-end -->

<!-- phoenix:html-start -->
## Phoenix HTML guidelines

- Phoenix templates **always** use `~H` or .html.heex files (known as HEEx), **never** use `~E`
- **Always** use the imported `Phoenix.Component.form/1` and `Phoenix.Component.inputs_for/1` function to build forms. **Never** use `Phoenix.HTML.form_for` or `Phoenix.HTML.inputs_for` as they are outdated
- When building forms **always** use the already imported `Phoenix.Component.to_form/2` (`assign(socket, form: to_form(...))` and `<.form for={@form} id="msg-form">`), then access those forms in the template via `@form[:field]`
- **Always** add unique DOM IDs to key elements (like forms, buttons, etc) when writing templates, these IDs can later be used in tests (`<.form for={@form} id="product-form">`)
- For "app wide" template imports, you can import/alias into the `my_app_web.ex`'s `html_helpers` block, so they will be available to all LiveViews, LiveComponent's, and all modules that do `use MyAppWeb, :html` (replace "my_app" by the actual app name)

- Elixir supports `if/else` but **does NOT support `if/else if` or `if/elsif`**. **Never use `else if` or `elseif` in Elixir**, **always** use `cond` or `case` for multiple conditionals.

  **Never do this (invalid)**:

      <%= if condition do %>
        ...
      <% else if other_condition %>
        ...
      <% end %>

  Instead **always** do this:

      <%= cond do %>
        <% condition -> %>
          ...
        <% condition2 -> %>
          ...
        <% true -> %>
          ...
      <% end %>

- HEEx require special tag annotation if you want to insert literal curly's like `{` or `}`. If you want to show a textual code snippet on the page in a `<pre>` or `<code>` block you *must* annotate the parent tag with `phx-no-curly-interpolation`:

      <code phx-no-curly-interpolation>
        let obj = {key: "val"}
      </code>

  Within `phx-no-curly-interpolation` annotated tags, you can use `{` and `}` without escaping them, and dynamic Elixir expressions can still be used with `<%= ... %>` syntax

- HEEx class attrs support lists, but you must **always** use list `[...]` syntax. You can use the class list syntax to conditionally add classes, **always do this for multiple class values**:

      <a class={[
        "px-2 text-white",
        @some_flag && "py-5",
        if(@other_condition, do: "border-red-500", else: "border-blue-100"),
        ...
      ]}>Text</a>

  and **always** wrap `if`'s inside `{...}` expressions with parens, like done above (`if(@other_condition, do: "...", else: "...")`)

  and **never** do this, since it's invalid (note the missing `[` and `]`):

      <a class={
        "px-2 text-white",
        @some_flag && "py-5"
      }> ...
      => Raises compile syntax error on invalid HEEx attr syntax

- **Never** use `<% Enum.each %>` or non-for comprehensions for generating template content, instead **always** use `<%= for item <- @collection do %>`
- HEEx HTML comments use `<%!-- comment --%>`. **Always** use the HEEx HTML comment syntax for template comments (`<%!-- comment --%>`)
- HEEx allows interpolation via `{...}` and `<%= ... %>`, but the `<%= %>` **only** works within tag bodies. **Always** use the `{...}` syntax for interpolation within tag attributes, and for interpolation of values within tag bodies. **Always** interpolate block constructs (if, cond, case, for) within tag bodies using `<%= ... %>`.

  **Always** do this:

      <div id={@id}>
        {@my_assign}
        <%= if @some_block_condition do %>
          {@another_assign}
        <% end %>
      </div>

  and **Never** do this – the program will terminate with a syntax error:

      <%!-- THIS IS INVALID NEVER EVER DO THIS --%>
      <div id="<%= @invalid_interpolation %>">
        {if @invalid_block_construct do}
        {end}
      </div>
<!-- phoenix:html-end -->

<!-- phoenix:liveview-start -->
## Phoenix LiveView guidelines

- **Never** use the deprecated `live_redirect` and `live_patch` functions, instead **always** use the `<.link navigate={href}>` and  `<.link patch={href}>` in templates, and `push_navigate` and `push_patch` functions LiveViews
- **Avoid LiveComponent's** unless you have a strong, specific need for them
- LiveViews should be named like `AppWeb.WeatherLive`, with a `Live` suffix. When you go to add LiveView routes to the router, the default `:browser` scope is **already aliased** with the `AppWeb` module, so you can just do `live "/weather", WeatherLive`

### LiveView streams

- **Always** use LiveView streams for collections for assigning regular lists to avoid memory ballooning and runtime termination with the following operations:
  - basic append of N items - `stream(socket, :messages, [new_msg])`
  - resetting stream with new items - `stream(socket, :messages, [new_msg], reset: true)` (e.g. for filtering items)
  - prepend to stream - `stream(socket, :messages, [new_msg], at: -1)`
  - deleting items - `stream_delete(socket, :messages, msg)`

- When using the `stream/3` interfaces in the LiveView, the LiveView template must 1) always set `phx-update="stream"` on the parent element, with a DOM id on the parent element like `id="messages"` and 2) consume the `@streams.stream_name` collection and use the id as the DOM id for each child. For a call like `stream(socket, :messages, [new_msg])` in the LiveView, the template would be:

      <div id="messages" phx-update="stream">
        <div :for={{id, msg} <- @streams.messages} id={id}>
          {msg.text}
        </div>
      </div>

- LiveView streams are *not* enumerable, so you cannot use `Enum.filter/2` or `Enum.reject/2` on them. Instead, if you want to filter, prune, or refresh a list of items on the UI, you **must refetch the data and re-stream the entire stream collection, passing reset: true**:

      def handle_event("filter", %{"filter" => filter}, socket) do
        # re-fetch the messages based on the filter
        messages = list_messages(filter)

        {:noreply,
         socket
         |> assign(:messages_empty?, messages == [])
         # reset the stream with the new messages
         |> stream(:messages, messages, reset: true)}
      end

- LiveView streams *do not support counting or empty states*. If you need to display a count, you must track it using a separate assign. For empty states, you can use Tailwind classes:

      <div id="tasks" phx-update="stream">
        <div class="hidden only:block">No tasks yet</div>
        <div :for={{id, task} <- @streams.tasks} id={id}>
          {task.name}
        </div>
      </div>

  The above only works if the empty state is the only HTML block alongside the stream for-comprehension.

- When updating an assign that should change content inside any streamed item(s), you MUST re-stream the items
  along with the updated assign:

      def handle_event("edit_message", %{"message_id" => message_id}, socket) do
        message = Chat.get_message!(message_id)
        edit_form = to_form(Chat.change_message(message, %{content: message.content}))

        # re-insert message so @editing_message_id toggle logic takes effect for that stream item
        {:noreply,
         socket
         |> stream_insert(:messages, message)
         |> assign(:editing_message_id, String.to_integer(message_id))
         |> assign(:edit_form, edit_form)}
      end

  And in the template:

      <div id="messages" phx-update="stream">
        <div :for={{id, message} <- @streams.messages} id={id} class="flex group">
          {message.username}
          <%= if @editing_message_id == message.id do %>
            <%!-- Edit mode --%>
            <.form for={@edit_form} id="edit-form-#{message.id}" phx-submit="save_edit">
              ...
            </.form>
          <% end %>
        </div>
      </div>

- **Never** use the deprecated `phx-update="append"` or `phx-update="prepend"` for collections

### LiveView JavaScript interop

- Remember anytime you use `phx-hook="MyHook"` and that JS hook manages its own DOM, you **must** also set the `phx-update="ignore"` attribute
- **Always** provide an unique DOM id alongside `phx-hook` otherwise a compiler error will be raised

LiveView hooks come in two flavors, 1) colocated js hooks for "inline" scripts defined inside HEEx,
and 2) external `phx-hook` annotations where JavaScript object literals are defined and passed to the `LiveSocket` constructor.

#### Inline colocated js hooks

**Never** write raw embedded `<script>` tags in heex as they are incompatible with LiveView.
Instead, **always use a colocated js hook script tag (`:type={Phoenix.LiveView.ColocatedHook}`)
when writing scripts inside the template**:

    <input type="text" name="user[phone_number]" id="user-phone-number" phx-hook=".PhoneNumber" />
    <script :type={Phoenix.LiveView.ColocatedHook} name=".PhoneNumber">
      export default {
        mounted() {
          this.el.addEventListener("input", e => {
            let match = this.el.value.replace(/\D/g, "").match(/^(\d{3})(\d{3})(\d{4})$/)
            if(match) {
              this.el.value = `${match[1]}-${match[2]}-${match[3]}`
            }
          })
        }
      }
    </script>

- colocated hooks are automatically integrated into the app.js bundle
- colocated hooks names **MUST ALWAYS** start with a `.` prefix, i.e. `.PhoneNumber`

#### External phx-hook

External JS hooks (`<div id="myhook" phx-hook="MyHook">`) must be placed in `assets/js/` and passed to the
LiveSocket constructor:

    const MyHook = {
      mounted() { ... }
    }
    let liveSocket = new LiveSocket("/live", Socket, {
      hooks: { MyHook }
    });

#### Pushing events between client and server

Use LiveView's `push_event/3` when you need to push events/data to the client for a phx-hook to handle.
**Always** return or rebind the socket on `push_event/3` when pushing events:

    # re-bind socket so we maintain event state to be pushed
    socket = push_event(socket, "my_event", %{...})

    # or return the modified socket directly:
    def handle_event("some_event", _, socket) do
      {:noreply, push_event(socket, "my_event", %{...})}
    end

Pushed events can then be picked up in a JS hook with `this.handleEvent`:

    mounted() {
      this.handleEvent("my_event", data => console.log("from server:", data));
    }

Clients can also push an event to the server and receive a reply with `this.pushEvent`:

    mounted() {
      this.el.addEventListener("click", e => {
        this.pushEvent("my_event", { one: 1 }, reply => console.log("got reply from server:", reply));
      })
    }

Where the server handled it via:

    def handle_event("my_event", %{"one" => 1}, socket) do
      {:reply, %{two: 2}, socket}
    end

### LiveView tests

- `Phoenix.LiveViewTest` module and `LazyHTML` (included) for making your assertions
- Form tests are driven by `Phoenix.LiveViewTest`'s `render_submit/2` and `render_change/2` functions
- Come up with a step-by-step test plan that splits major test cases into small, isolated files. You may start with simpler tests that verify content exists, gradually add interaction tests
- **Always reference the key element IDs you added in the LiveView templates in your tests** for `Phoenix.LiveViewTest` functions like `element/2`, `has_element/2`, selectors, etc
- **Never** tests again raw HTML, **always** use `element/2`, `has_element/2`, and similar: `assert has_element?(view, "#my-form")`
- Instead of relying on testing text content, which can change, favor testing for the presence of key elements
- Focus on testing outcomes rather than implementation details
- Be aware that `Phoenix.Component` functions like `<.form>` might produce different HTML than expected. Test against the output HTML structure, not your mental model of what you expect it to be
- When facing test failures with element selectors, add debug statements to print the actual HTML, but use `LazyHTML` selectors to limit the output, ie:

      html = render(view)
      document = LazyHTML.from_fragment(html)
      matches = LazyHTML.filter(document, "your-complex-selector")
      IO.inspect(matches, label: "Matches")

### Form handling

#### Creating a form from params

If you want to create a form based on `handle_event` params:

    def handle_event("submitted", params, socket) do
      {:noreply, assign(socket, form: to_form(params))}
    end

When you pass a map to `to_form/1`, it assumes said map contains the form params, which are expected to have string keys.

You can also specify a name to nest the params:

    def handle_event("submitted", %{"user" => user_params}, socket) do
      {:noreply, assign(socket, form: to_form(user_params, as: :user))}
    end

#### Creating a form from changesets

When using changesets, the underlying data, form params, and errors are retrieved from it. The `:as` option is automatically computed too. E.g. if you have a user schema:

    defmodule MyApp.Users.User do
      use Ecto.Schema
      ...
    end

And then you create a changeset that you pass to `to_form`:

    %MyApp.Users.User{}
    |> Ecto.Changeset.change()
    |> to_form()

Once the form is submitted, the params will be available under `%{"user" => user_params}`.

In the template, the form form assign can be passed to the `<.form>` function component:

    <.form for={@form} id="todo-form" phx-change="validate" phx-submit="save">
      <.input field={@form[:field]} type="text" />
    </.form>

Always give the form an explicit, unique DOM ID, like `id="todo-form"`.

#### Avoiding form errors

**Always** use a form assigned via `to_form/2` in the LiveView, and the `<.input>` component in the template. In the template **always access forms this**:

    <%!-- ALWAYS do this (valid) --%>
    <.form for={@form} id="my-form">
      <.input field={@form[:field]} type="text" />
    </.form>

And **never** do this:

    <%!-- NEVER do this (invalid) --%>
    <.form for={@changeset} id="my-form">
      <.input field={@changeset[:field]} type="text" />
    </.form>

- You are FORBIDDEN from accessing the changeset in the template as it will cause errors
- **Never** use `<.form let={f} ...>` in the template, instead **always use `<.form for={@form} ...>`**, then drive all form references from the form assign as in `@form[:field]`. The UI should **always** be driven by a `to_form/2` assigned in the LiveView module that is derived from a changeset
<!-- phoenix:liveview-end -->

<!-- usage-rules-end -->