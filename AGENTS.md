# AGENTS.md — Charset Playground

## Produit

Exercices interactifs pour apprendre **l'encodage et le décodage** des caractères :
ASCII, Latin-1, Windows-1252, UTF-8, UTF-16, UTF-32, endianness, BOM.

- **Sandbox** : 10 pages (encode/decode × utf-8, utf-16, utf-32, latin1, windows-1252)
  où l'utilisateur saisit un caractère/des bytes et voit la conversion décomposée étape
  par étape, avec feedback immédiat à la frappe.
- **Exercices** : 6 modules jouables (encode/decode × utf-8, utf-16, utf-32) où
  l'utilisateur fait les conversions à la main, step par step, avec validation
  immédiate côté serveur, un hint par type d'erreur, reveal après 3 essais, et
  progression persistée (niveau auto-avancé par streak de 5).
- **Pas de compte utilisateur** : tout est keyé par un token anonyme opaque (UUID) dans
  un cookie HttpOnly. Ne pas réintroduire d'auth.

### Historique

Le projet est une réécriture Phoenix/LiveView (terminée le 2026-06-06) d'une stack
Kotlin 2.x + Spring Boot 4 (API REST) + Nuxt 4 / Vue 3 (~18 000 lignes). L'ancienne
implémentation reste consultable sur la branche **`kotlin-nuxt`**
(`git show kotlin-nuxt:src/main/kotlin/...`, `git show kotlin-nuxt:web/app/...`) - elle fait foi
uniquement comme archéologie, le code actuel est la référence.

## Workflow git

- Branches de travail depuis `main`, PRs vers `main` (`gh pr create`).
- `mix precommit` doit passer avant toute PR.
- Reviews Copilot : vérifier ses affirmations sur la stdlib Elixir avant d'obtempérer
  (historique de faux positifs : casse de `Integer.to_string/2`, sémantique multi-`when`,
  captures Regex) ; ses remarques de cohérence produit sont en revanche souvent bonnes.

## Stack

| Brique | Choix | Version |
|---|---|---|
| Langage | Elixir | **1.20** (OTP 29) |
| Framework | Phoenix | **1.8.7** |
| UI temps réel | Phoenix LiveView | 1.1.x |
| HTTP server | Bandit | dernière compatible |
| DB | SQLite (mode WAL) | via `ecto_sqlite3` |
| Backups | aucun automatisé (décision 2026-06-06 : progression anonyme = enjeu faible) ; sauvegarder le volume `/data` côté hôte si besoin | — |
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

`Charset.Encoding.Codec` est écrit dans ce style - le conserver pour toute évolution.

### Typage — exploiter le type system d'Elixir 1.20 au maximum

Elixir 1.20 marque le premier jalon production-ready du type system set-theoretic :
**inférence automatique sur tout le code, sans annotation** (voir
https://elixir-lang.org/blog/2026/06/03/elixir-v1-20-0-released/). Le compilateur
infère les types depuis les guards, le pattern matching et le control flow, et
remonte les violations garanties d'échouer au runtime.

Les **signatures de types écrites par le développeur n'existent pas encore en 1.20**
(elles viendront avec les typed structs puis les signatures). « Utiliser les types au
maximum » signifie donc écrire du code que l'inférence peut exploiter :

- **Toutes les violations de typage sont des erreurs.** `mix compile
  --warnings-as-errors` dans `mix precommit` et en CI — un warning de typage ne se
  merge pas.
- **Guards systématiques** sur les fonctions publiques du domaine (`when is_integer(cp)
  and cp >= 0 and cp <= 0x10FFFF`) : à la fois validation des invariants et information
  de type pour l'inférence.
- **Structs partout, maps nues nulle part** dans le domaine : `%Step.Binary{}`,
  `%ValidationResult{}`, etc. avec `@enforce_keys` sur les champs obligatoires.
- **Pattern matching plutôt qu'accès dynamique** : `%Step.Binary{expected: expected} =
  step` plutôt que `step.expected` sur un type incertain, `case`/`with` avec clauses
  exhaustives.
- **Pas de `dynamic()` volontaire** : éviter les constructions qui forcent le
  compilateur à abandonner le narrowing (maps hétérogènes fourre-tout, `apply/3`,
  `Map.get` sur des structs, `String.to_existing_atom` là où un ensemble fermé de
  clauses suffit).
- **`@spec` sur l'API publique des contexts et du domaine** : documentation +
  préparation des vraies signatures à venir.
- Quand une nouvelle version d'Elixir étend le type system (typed structs, signatures),
  **adopter immédiatement** sur `Charset.Encoding` et `Charset.Exercise` — le domaine
  est petit, pur et entièrement testé, c'est le candidat idéal.

## Architecture

Phoenix Contexts classiques. Le domaine reste pur (pas d'Ecto, pas de Phoenix dans les
modules de calcul).

**Namespaces** : l'OTP app s'appelle **`:app`**. `App`/`AppWeb` portent
l'infrastructure (Application, Repo, endpoint, LiveViews) ; les **domaines** sont des
namespaces top-level à côté - `Charset.*` aujourd'hui (encoding, exercise, sandbox,
progress), d'autres demain (ex. `Binary.*`) sans toucher à la couche web.

```
lib/
  charset/                          # Domaine charset (pur, pas d'Ecto ni Phoenix)
    encoding/                       # codec.ex (bitstrings !), code_point.ex, bytes.ex,
                                    # encoding.ex, windows1252.ex, {encode,decode}_error.ex
    exercise/                       # Pur : step/ (8 types), answer.ex, answer_validator.ex,
                                    # validation_result.ex, error_type.ex, param_key.ex,
                                    # format_choice.ex, exercise_module.ex, generator/,
                                    # attempt.ex, attempt_step.ex (structs domain),
                                    # service.ex (orchestration : génération, validation,
                                    # reveal, finalisation, garde de propriété par token)
    sandbox/                        # Pur : parsers d'input + labels + décomposition
    progress/                       # module_progress.ex (struct + streak/level-up)
    schema/                         # Schemas Ecto (un fichier par table)
    exercise_attempts.ex            # Context Ecto : attempts + steps (table-per-type)
    progress.ex                     # Context Ecto : module_progress (token, module_id)
  app/                              # Infrastructure app-wide (App.*)
    application.ex                  # OTP supervision tree (inclut Ecto.Migrator au boot)
    repo.ex                         # App.Repo (SQLite)
  app_web/                          # Couche web partagée par tous les domaines (AppWeb.*)
    router.ex                       # scopes / et /fr, live_sessions par locale
    locale.ex                       # localized_path/2, alternate_path/2
    plugs/locale.ex                 # plug HTTP locale
    plugs/visitor_token.ex          # mint/lit le cookie token anonyme HttpOnly (UUID only)
    live/locale_hook.ex             # on_mount : Gettext + :locale sur le websocket
    live/sandbox/                   # 10 LiveViews (encode/decode × 5 encodings)
    live/exercise_live.ex           # 1 LiveView, 6 modules via live actions
    controllers/                    # landing (statique, SEO), redirect /sandbox
    components/                     # layouts, core, sandbox_components, exercise_components
assets/                             # Vite + Tailwind v4 (package.json à la racine du repo)
priv/repo/migrations/
priv/gettext/{fr,en}/LC_MESSAGES/   # default.po + labels.po + feedback.po
test/                               # Miroir de lib/
```

### Anti-cheat — par construction avec LiveView

Règle invariante : **la valeur attendue (`expected`) ne traverse jamais le réseau vers
le client** (sauf reveal explicite).

`expected` vit dans les assigns du process serveur et en DB
(`attempt_step_<type>.expected`). Vigilance :

- Ne jamais interpoler `expected` dans le HEEx (même dans un attribut `data-*` ou un
  commentaire HTML). Le test « anti-cheat » de `exercise_live_test.exs` le pinne.
- `ValidationResult.params` peut contenir des hints structurels (`expected_length`,
  `expected_count`, `position`, bornes) et le `got` de l'utilisateur — **jamais** la
  valeur canonique attendue. Cas particulier `endianness` : 2 choix possibles, donc même
  le `got` révèle la réponse — aucun params.
- La DB reste la source de vérité pour la validation (defense in depth) : on lit
  `expected` depuis la row `attempt_step_<type>`, jamais depuis ce qu'envoie le client.
- La sandbox est exemptée : c'est un visualiseur, elle révèle tout par design.

### Hints et reveal

1 hint par `error_type` (domaine gettext `feedback`, clé = l'identifiant stable), avec
compteur d'essais affiché (`Try n/3`). Le bouton « Show me the answer » n'apparaît
qu'au seuil de 3 essais sur le step (gate serveur dans `Exercise.Service`, pas
seulement UI). Un step révélé rend l'attempt entier incorrect ; la complétion est
enregistrée sur la progression dans tous les cas.

## Modèle de données

- `exercise_attempts` (token, module_id, level, code_point, encoding, correct,
  finalized, duration_ms) + index `(token, module_id)`
- `attempt_steps` (attempt_id, position, step_type, correct, error_type, attempts,
  revealed) + UNIQUE(attempt_id, position)
- 8 tables filles table-per-type : `attempt_step_format`, `_binary`, `_bit_groups`,
  `_hex_bytes`, `_code_point`, `_useful_bit_count`, `_offset`, `_endianness` —
  chacune porte `expected` et `user_answer`
- `module_progress` (token, module_id, level, streak, attempts, errors,
  last_played_at) + UNIQUE(token, module_id)

Conventions DB :
- **Pas de CHECK constraints sur les valeurs énumérées** (l'app est le seul writer, les
  enums Elixir sont la source de vérité). Les FK restent (`PRAGMA foreign_keys=ON`,
  activé par `ecto_sqlite3`).
- Les séquences de bits/bytes sont des champs Ecto `{:array, :string}` /
  `{:array, :integer}`, stockés en JSON text par `ecto_sqlite3`. Le domaine ne query
  jamais ces séquences par position (toujours lues en bloc par le validator).
- Timestamps UTC. Migrations générées par `mix ecto.gen.migration`, appliquées au boot
  par l'`Ecto.Migrator` de l'arbre de supervision.

### Réglages SQLite

- **Mode WAL** (défaut `ecto_sqlite3`) : lecteurs et writer ne se bloquent pas.
- `busy_timeout` raisonnable pour absorber la contention single-writer.
- Le fichier DB vit dans un répertoire dédié monté en volume Docker (`/data/charset.db`),
  configuré via `DATABASE_PATH` dans `config/runtime.exs`.

## i18n

- **Gettext**, **EN locale par défaut** (msgids = texte anglais), FR sous le préfixe
  `/fr/...`. Plug `AppWeb.Plugs.Locale` (HTTP) + hook `AppWeb.LocaleHook` (websocket),
  scopes router dupliqués `/` et `/fr`, helper `localized_path/2`.
- Trois catalogues : `default.po` (extrait du code), `labels.po` (mnémoniques Unicode,
  liste fermée maintenue à la main, lookup dynamique) et `feedback.po` (hints
  d'exercice, clé = `error_type` stable, liste fermée).
- Les `error_type` produits par le domaine (`binary.wrong-value`, etc.) sont des
  **identifiants stables** déclarés comme constantes dans `error_type.ex` — jamais de
  string literal au call site (ni en prod ni en test).
- **Mini-markup InlineDesc** dans les catalogues : `` `code` `` → `<code>`,
  `[texte^titre]` → `<abbr title>`, `\n` → `<br>`. Rendu par
  `SandboxComponents.inline_desc/1` avec échappement **par token**. Ne pas remplacer
  par du HTML littéral dans les .po : on perdrait l'échappement et les phrases
  complètes traduisibles (décision du 2026-06-05).

### Conventions de contenu user-facing (NON négociables)

- **Tutoiement en FR**, toujours.
- **Tiret simple `-`, jamais d'em dash `—`** dans tout contenu visible par
  l'utilisateur (templates HEEx, fichiers gettext). Les commentaires de code ne sont
  pas concernés.
- **Les commentaires de code sont en anglais**, même si la doc projet est en français.

## Design / front

- Le design vit dans `assets/css/app.css` (tokens light/dark via `.dark`, composants
  `.bit`, `.btn`, `.surface`, etc.) et les composants HEEx. `bit-sm` est réservé au
  sandbox (cellules resserrées) ; l'exercice utilise les cellules pleine taille.
- Composants maison (pas de lib UI) : menus header (`assets/js/menu.js`), toggles
  theme/locale, widgets d'exercice à cellules (`assets/js/exercise_hooks.js` :
  BitCells, HexCells, FilteredInput - auto-avance, flèches, ↑/↓ = 1/0).
- Sandbox : feedback à la frappe via `phx-change` + debounce, état dans l'URL
  (`push_patch` + `replace: true`, liens partageables).
- Exercice : conteneurs de saisie en `phx-update="ignore"` (la frappe survit aux
  patches), ids remontés par attempt+step pour forcer le remount au changement.
- Meta OG/Twitter + image sociale en place ; dark/light theme et locale toggle.

## Assets — Vite + Tailwind v4

- `package.json` à la racine avec `vite`, `tailwindcss` 4.x, `@tailwindcss/vite`.
- Tailwind v4 : config par CSS (`@theme` dans `assets/css/app.css`), pas de
  `tailwind.config.js`.
- Dev : watcher Vite déclaré dans `config/dev.exs`, HMR.
- Prod : Vite émet dans `priv/static/assets/`, puis `mix phx.digest`.
- **Le build Vite dépend d'un projet mix compilé** : les aliases résolvent les clients
  JS Phoenix depuis `deps/` et les colocated hooks depuis
  `_build/${MIX_ENV}/phoenix-colocated/`. D'où le builder Docker unique (voir Docker).

## Docker

- **Builder unique Elixir + Node** (apk) — pas de stage assets séparé style tracker-tv,
  car Vite a besoin de `mix compile` au préalable (voir Assets). Ordre :
  `deps.get` → `deps.compile` → `npm ci` → `mix compile` → `npm run build` →
  `phx.digest` → `mix release`. Le layer `npm ci` est placé avant le COPY du code pour
  rester en cache.
- Node du builder = celui d'apk (≈ v22), pas le `.nvmrc` (26) : divergence connue et
  acceptée, la sortie Vite est identique.
- **Runtime** : Alpine nu + `libstdc++ openssl ncurses-libs ca-certificates`, user
  non-root, `ENV LANG=C.UTF-8`, volume `/data`, `EXPOSE 4000`, `CMD ["bin/app", "start"]`.
- Migrations au boot (Ecto.Migrator), pas de commande de release à orchestrer.
- Runtime config via env vars (`config/runtime.exs`) : `DATABASE_PATH`,
  `SECRET_KEY_BASE`, `PHX_HOST`, `PORT`. `PHX_SERVER=true` est posé dans l'image.
- `compose.prod.yml` : un service app + volume `appdata:/data`, pas de service DB.
  Caddy reste le reverse proxy sur l'hôte.
- **Pas de backup automatisé** (décision 2026-06-06) : la DB ne contient que la
  progression anonyme, l'enjeu ne justifie pas l'infra. Si ça change, Litestream
  (réplication continue du WAL, supervision `litestream replicate -exec`) est le
  candidat naturel - il a existé dans l'image jusqu'à la PR #71, voir l'historique.
- CI image : `.github/workflows/docker.yml` — push ghcr (`latest` + `sha-<short>` +
  semver sur tags `v*`) depuis `main` uniquement.

## Tests

- **ExUnit** partout, ~560 tests. Doctests bienvenus sur le codec.
- Couverture minimale à maintenir :
  - `Charset.Encoding.Codec` : tous les cas frontières — U+007F, U+0080, U+07FF,
    U+0800, U+FFFF, U+10000, U+10FFFF, surrogates — pour encode ET decode, chaque
    encoding, chaque endianness. Sweeps exhaustifs contre les encodeurs natifs BEAM
    comme oracle.
  - `AnswerValidator` : chaque `error_type` produit avec les bons `params`.
  - Contexts Ecto : upsert, find, contraintes uniques (via la DB de test, pas de mocks).
  - LiveViews : flows sandbox et exercice complets, incluant « `expected` n'apparaît
    jamais dans le HTML rendu ».
- Sandbox DB : `Ecto.Adapters.SQL.Sandbox` sur SQLite. Contrainte single-writer : les
  tests qui touchent la DB restent en `async: false` ; les tests du domaine pur (la
  grande majorité) restent async.

## Commandes

```bash
mix setup            # deps + DB + npm install
mix phx.server       # dev server (lance le watcher Vite)
mix test             # tests
mix precommit        # alias : compile --warnings-as-errors + format --check + test
npm run build        # build Vite de prod
docker build .       # image complète (builder Elixir+Node, runtime Alpine)
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