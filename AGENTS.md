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
| Langage | Elixir | **1.20** (OTP 28) |
| Framework | Phoenix | **1.8.7** |
| UI temps réel | Phoenix LiveView | **1.1.30** |
| HTTP server | Bandit | dernière compatible |
| DB | SQLite (mode WAL) | via `ecto_sqlite3` |
| Backups | Litestream (réplication continue du WAL vers un stockage objet) | — |
| ORM / migrations | Ecto + ecto_sql + ecto_sqlite3 | dernière compatible |
| CSS | Tailwind CSS v4 **via Vite** | tailwind 4.x + vite |
| i18n | Gettext (FR par défaut, EN) | `{:gettext, "~> 1.0"}` |
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

```
lib/
  charset/                          # Domaine + persistance (contexts)
    encoding/                       # Pur : codec, specs d'encodage
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
    repo.ex
  charset_web/
    router.ex
    plugs/visitor_token.ex          # pose/lit le cookie token anonyme HttpOnly
    live/
      sandbox/                      # 10 LiveViews (encode/decode × 5 encodings)
      exercise_live.ex              # flow d'exercice step-by-step
    controllers/page_controller.ex  # landing (statique, SEO)
    components/                     # core_components + composants portés depuis Vue
assets/                             # Vite + Tailwind v4 (package.json à la racine du repo ou dans assets/)
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

- **Gettext**, FR locale par défaut, EN en second. Routes EN préfixées `/en/...`
  (équivalent de la stratégie `prefix_except_default` de l'ancien front).
- Porter les locales depuis `main:web/i18n/locales/{fr,en}/*.json` (~1 100 lignes).
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
  les recréer en HEEx + Tailwind dans `charset_web/components/`.
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
2. **Stage builder** : `elixir:1.20-otp-28-alpine` → `mix deps.get --only prod` →
   compile → `mix phx.digest` → `mix release`
3. **Stage runtime** : `alpine` nu + `libstdc++ openssl ncurses-libs ca-certificates`,
   user non-root, `ENV LANG=C.UTF-8`, `EXPOSE 4000`, `CMD ["bin/charset", "start"]`

Runtime config via env vars dans `config/runtime.exs` : `DATABASE_PATH` (fichier SQLite
sur un volume), `SECRET_KEY_BASE`, `PHX_HOST`, `PORT`. Caddy reste le reverse proxy
devant (un seul backend désormais, plus de split :3000/:8080).

Pas de service DB dans le compose : SQLite est embarqué, le fichier vit sur un volume.
**Backups via Litestream** : réplication continue du WAL vers un stockage objet
(R2/B2/S3), soit en sidecar dans le compose, soit en superviseur du release
(`litestream replicate -exec "bin/charset start"`). Restauration point-in-time via
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
- [ ] **Phase 2 — Domaine exercise** : steps, answers, `AnswerValidator` (chaque
      `error_type` testé), generators par encoding × niveau, hints gradués.
- [ ] **Phase 3 — Persistance** : migrations Ecto (schéma de main, arrays adaptés en
      JSON), schemas, contexts attempts + progress, tests d'intégration DB.
- [ ] **Phase 4 — Sandbox LiveView** : les 10 pages, parsers portés, feedback à la
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
