defmodule AppWeb.Plugs.Locale do
  @moduledoc """
  Sets the Gettext locale for the request.

  The locale is carried by the URL - EN (default) lives at the root, FR under
  the `/fr` prefix - mirroring the `prefix_except_default` strategy of the old
  Nuxt frontend on `main`. The router pipes each scope through this plug with
  the scope's locale.

  Assigns:

    * `:locale` - `"en"` | `"fr"`, used for `<html lang>` and locale-aware links
    * `:alternate_path` - the same page in the other locale, used by the
      header locale toggle
  """

  import Plug.Conn

  alias AppWeb.Locale

  def init(locale) when locale in ["en", "fr"], do: locale

  def call(conn, locale) do
    Gettext.put_locale(AppWeb.Gettext, locale)

    conn
    |> assign(:locale, locale)
    |> assign(:alternate_path, Locale.alternate_path(conn.request_path, locale))
  end
end
