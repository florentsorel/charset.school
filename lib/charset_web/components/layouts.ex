defmodule CharsetWeb.Layouts do
  @moduledoc """
  This module holds layouts and related functionality
  used by your application.
  """
  use CharsetWeb, :html

  # Embed all files in layouts/* within this module.
  # The default root.html.heex file contains the HTML
  # skeleton of your application, namely HTML headers
  # and other static content.
  embed_templates "layouts/*"

  @doc """
  Renders your app layout: sticky header, content, footer.

  ## Examples

      <Layouts.app flash={@flash} locale={@locale} alternate_path={@alternate_path}>
        <h1>Content</h1>
      </Layouts.app>

  """
  attr :flash, :map, required: true, doc: "the map of flash messages"
  attr :locale, :string, required: true, doc: "the current locale (en | fr)"

  attr :alternate_path, :string,
    required: true,
    doc: "the same page in the other locale, linked by the locale toggle"

  slot :inner_block, required: true

  def app(assigns) do
    ~H"""
    <div class="min-h-dvh flex flex-col">
      <.app_header locale={@locale} alternate_path={@alternate_path} />
      <div class="flex-1 flex flex-col">
        {render_slot(@inner_block)}
      </div>
      <.app_footer />
    </div>
    <.flash_group flash={@flash} />
    """
  end

  attr :locale, :string, required: true
  attr :alternate_path, :string, required: true

  defp app_header(assigns) do
    ~H"""
    <header class="sticky top-0 z-40 bg-page/85 backdrop-blur border-b border-rule h-[var(--header-height)]">
      <div class="mx-auto max-w-6xl px-4 sm:px-6 h-full flex items-center justify-between gap-4">
        <div class="flex items-center gap-3 sm:gap-5">
          <a
            href={localized_path(@locale, "/")}
            class="flex items-center gap-2 hover:opacity-80 transition-opacity"
          >
            <img src={~p"/logo.svg"} width="22" height="13" alt="charset.school" aria-hidden="true" />
            <span class="font-mono text-md leading-none lowercase">charset.school</span>
          </a>

          <%!-- Desktop "Exercises" trigger (≥ lg). Toggles a full-width panel
               below the header. Below lg it lives in the burger. --%>
          <div class="hidden lg:block">
            <button
              type="button"
              class="btn btn-ghost text-sm gap-1"
              aria-haspopup="true"
              aria-controls="header-exercises-menu"
              aria-expanded="false"
              data-menu-toggle="header-exercises-menu-wrap"
            >
              {gettext("Exercises")}
              <.icon name="hero-chevron-down" class="w-4 h-4 transition-transform menu-chevron" />
            </button>
          </div>
        </div>

        <%!-- Desktop nav (≥ lg) --%>
        <div class="hidden lg:flex items-center gap-2.5">
          <a href={localized_path(@locale, "/sandbox")} class="btn btn-soft text-sm">
            {gettext("Sandbox")}
          </a>

          <.theme_toggle />
          <.locale_toggle locale={@locale} alternate_path={@alternate_path} />
        </div>

        <%!-- Burger cluster (< lg): theme + locale pills next to the menu button.
             The dropdown panel is full viewport width, anchored under the header.
             Holds exercises (in-place accordion) + sandbox. --%>
        <div class="lg:hidden flex items-center gap-2">
          <.theme_toggle />
          <.locale_toggle locale={@locale} alternate_path={@alternate_path} />
          <button
            type="button"
            class="hamburger-btn"
            aria-label={gettext("Menu")}
            aria-controls="header-burger-menu"
            aria-expanded="false"
            data-menu-toggle="header-burger-menu"
          >
            <.icon name="hero-bars-3" class="w-5 h-5" />
          </button>
        </div>
      </div>

      <%!-- Full-width "Exercises" mega-menu (desktop). Backdrop closes on click;
           Escape and link navigation close it too (assets/js/menu.js). --%>
      <div id="header-exercises-menu-wrap" hidden data-menu>
        <div class="fixed inset-0 z-30" aria-hidden="true" data-menu-close></div>
        <nav
          id="header-exercises-menu"
          class="fixed left-0 right-0 z-40 bg-page border-b border-rule shadow-sm top-[var(--header-height)]"
          aria-label={gettext("Exercises")}
        >
          <div class="mx-auto max-w-6xl px-4 sm:px-6 py-6">
            <div class="grid grid-cols-2 lg:grid-cols-3 gap-3">
              <a
                :for={module <- exercise_modules()}
                href={localized_path(@locale, module.path)}
                class="block p-4 rounded-md border border-rule hover:border-rule-strong transition-colors"
              >
                <div class="text-sm font-medium leading-tight">{module.title}</div>
                <p class="text-xs text-mute mt-1">{module.subtitle}</p>
              </a>
            </div>
          </div>
        </nav>
      </div>

      <%!-- Burger panel (< lg): full-width dropdown under the header --%>
      <div id="header-burger-menu" hidden data-menu>
        <div class="fixed inset-0 z-30" aria-hidden="true" data-menu-close></div>
        <div class="app-dropdown app-dropdown--full fixed left-0 right-0 z-40 top-[var(--header-height)]">
          <button
            type="button"
            class="app-dropdown-item w-full"
            aria-controls="burger-exercises-sub"
            aria-expanded="false"
            data-menu-toggle="burger-exercises-sub"
          >
            <.icon name="hero-code-bracket" class="app-dropdown-item-icon" />
            {gettext("Exercises")}
            <.icon name="hero-chevron-down" class="w-4 h-4 ml-auto transition-transform menu-chevron" />
          </button>
          <div id="burger-exercises-sub" hidden>
            <a
              :for={module <- exercise_modules()}
              href={localized_path(@locale, module.path)}
              class="app-dropdown-item app-dropdown-subitem"
            >
              {module.title}
            </a>
          </div>
          <div class="app-dropdown-separator"></div>
          <a href={localized_path(@locale, "/sandbox")} class="app-dropdown-item">
            <.icon name="hero-beaker" class="app-dropdown-item-icon" />
            {gettext("Sandbox")}
          </a>
        </div>
      </div>
    </header>
    """
  end

  defp app_footer(assigns) do
    ~H"""
    <footer class="border-t border-rule mt-auto">
      <div class="mx-auto max-w-6xl px-4 sm:px-6 py-10 text-sm text-mute">
        <div class="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <div class="max-w-md">
            <div class="font-mono text-base text-ink mb-2">
              charset.school
            </div>
            <p class="text-xs leading-relaxed">
              {gettext("Teaching tool. No tracking, no ads.")}
            </p>
          </div>
          <p class="text-xs text-mute sm:text-right">
            {gettext("Developed by")}
            <a
              href="https://sorel.dev"
              target="_blank"
              rel="noopener noreferrer"
              class="text-ink hover:text-accent transition-colors"
            >
              Florent Sorel
            </a>
          </p>
        </div>
      </div>
    </footer>
    """
  end

  @doc """
  Theme toggle pill: flips between light and dark. The first click pins the
  opposite of whatever is currently shown (`system` preference drops out once
  the user chooses) - the inline script in root.html.heex owns the logic.
  """
  def theme_toggle(assigns) do
    ~H"""
    <button
      type="button"
      class="lang-pill"
      aria-label={gettext("Theme")}
      title={gettext("Theme")}
      phx-click={JS.dispatch("charset:toggle-theme")}
    >
      <.icon name="hero-sun" class="w-4 h-4 dark:hidden" />
      <.icon name="hero-moon" class="w-4 h-4 hidden dark:inline-block" />
    </button>
    """
  end

  @doc """
  Locale toggle pill: shows the active locale; clicking navigates to the same
  page in the other locale (EN routes are unprefixed, FR routes under /fr).
  """
  attr :locale, :string, required: true
  attr :alternate_path, :string, required: true

  def locale_toggle(assigns) do
    ~H"""
    <a
      href={@alternate_path}
      class="lang-pill"
      aria-label={gettext("Language")}
      title={gettext("Language")}
    >
      <span>{String.upcase(@locale)}</span>
    </a>
    """
  end

  @doc """
  The 6 playable exercise modules, shared by the header "Exercises" menu and
  the home page grid. Titles/subtitles are translated at render time.
  """
  def exercise_modules do
    [
      %{
        id: "utf8-encode",
        path: "/exercise/encode/utf-8",
        title: gettext("Encode UTF-8"),
        subtitle: gettext("code point → bytes")
      },
      %{
        id: "utf8-decode",
        path: "/exercise/decode/utf-8",
        title: gettext("Decode UTF-8"),
        subtitle: gettext("bytes → code point")
      },
      %{
        id: "utf16-encode",
        path: "/exercise/encode/utf-16",
        title: gettext("Encode UTF-16"),
        subtitle: gettext("endianness + BOM")
      },
      %{
        id: "utf16-decode",
        path: "/exercise/decode/utf-16",
        title: gettext("Decode UTF-16"),
        subtitle: gettext("surrogate pairs included")
      },
      %{
        id: "utf32-encode",
        path: "/exercise/encode/utf-32",
        title: gettext("Encode UTF-32"),
        subtitle: gettext("fixed 4 bytes, padding")
      },
      %{
        id: "utf32-decode",
        path: "/exercise/decode/utf-32",
        title: gettext("Decode UTF-32"),
        subtitle: gettext("explicit endianness, 4 bytes → code point")
      }
    ]
  end

  @doc """
  Default SEO/social description, shared by the meta tags in root.html.heex.
  """
  def seo_description do
    gettext(
      "Interactive exercises to learn character encoding - do the UTF-8, UTF-16 and UTF-32 conversions by hand, step by step, with instant feedback. Free, no account, no tracking."
    )
  end

  def seo_og_image_alt do
    gettext("charset.school - learn character encoding by hand")
  end

  @doc """
  Shows the flash group with standard titles and content.

  ## Examples

      <.flash_group flash={@flash} />
  """
  attr :flash, :map, required: true, doc: "the map of flash messages"
  attr :id, :string, default: "flash-group", doc: "the optional id of flash container"

  def flash_group(assigns) do
    ~H"""
    <div id={@id} aria-live="polite">
      <.flash kind={:info} flash={@flash} />
      <.flash kind={:error} flash={@flash} />

      <.flash
        id="client-error"
        kind={:error}
        title={gettext("We can't find the internet")}
        phx-disconnected={show(".phx-client-error #client-error") |> JS.remove_attribute("hidden")}
        phx-connected={hide("#client-error") |> JS.set_attribute({"hidden", ""})}
        hidden
      >
        {gettext("Attempting to reconnect")}
        <.icon name="hero-arrow-path" class="ml-1 size-3 motion-safe:animate-spin" />
      </.flash>

      <.flash
        id="server-error"
        kind={:error}
        title={gettext("Something went wrong!")}
        phx-disconnected={show(".phx-server-error #server-error") |> JS.remove_attribute("hidden")}
        phx-connected={hide("#server-error") |> JS.set_attribute({"hidden", ""})}
        hidden
      >
        {gettext("Attempting to reconnect")}
        <.icon name="hero-arrow-path" class="ml-1 size-3 motion-safe:animate-spin" />
      </.flash>
    </div>
    """
  end
end
