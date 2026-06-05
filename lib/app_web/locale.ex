defmodule AppWeb.Locale do
  @moduledoc """
  URL helpers for the locale-prefixed routes.

  EN (default locale) is unprefixed, FR lives under `/fr` - the equivalent of
  the old frontend's `useLocalePath()`.
  """

  @locales ~w(en fr)

  def locales, do: @locales

  @doc """
  Prefixes `path` for the given locale.

      iex> AppWeb.Locale.localized_path("en", "/sandbox")
      "/sandbox"

      iex> AppWeb.Locale.localized_path("fr", "/sandbox")
      "/fr/sandbox"

      iex> AppWeb.Locale.localized_path("fr", "/")
      "/fr"
  """
  def localized_path("en", path), do: path
  def localized_path("fr", "/"), do: "/fr"
  def localized_path("fr", path), do: "/fr" <> path

  @doc """
  The same path in the other locale: EN pages gain the /fr prefix, FR pages
  lose it. Used by the header locale toggle.

      iex> AppWeb.Locale.alternate_path("/sandbox", "en")
      "/fr/sandbox"

      iex> AppWeb.Locale.alternate_path("/fr/sandbox", "fr")
      "/sandbox"
  """
  def alternate_path(path, "en"), do: localized_path("fr", path)
  def alternate_path("/fr", "fr"), do: "/"
  def alternate_path("/fr/" <> rest, "fr"), do: "/" <> rest
  def alternate_path(path, "fr"), do: path
end
