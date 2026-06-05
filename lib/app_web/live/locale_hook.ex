defmodule AppWeb.LocaleHook do
  @moduledoc """
  LiveView counterpart of `AppWeb.Plugs.Locale`: plugs only run on the HTTP
  request, not on the websocket mount, so the locale is re-established here
  from the `live_session` static session map.

  Assigns `:locale`, and keeps `:alternate_path` in sync with the current URL
  (query string included) on every `handle_params`.
  """

  import Phoenix.Component
  import Phoenix.LiveView

  alias AppWeb.Locale

  def on_mount(:default, _params, session, socket) do
    locale = session["locale"] || "en"
    Gettext.put_locale(AppWeb.Gettext, locale)

    socket =
      socket
      |> assign(:locale, locale)
      |> attach_hook(:alternate_path, :handle_params, &assign_alternate_path/3)

    {:cont, socket}
  end

  defp assign_alternate_path(_params, uri, socket) do
    %URI{path: path, query: query} = URI.parse(uri)
    alternate = Locale.alternate_path(path, socket.assigns.locale)
    alternate = if query, do: alternate <> "?" <> query, else: alternate
    {:cont, assign(socket, :alternate_path, alternate)}
  end
end
