defmodule CharsetWeb.Locale do
  @moduledoc """
  URL helpers for the locale-prefixed routes.

  EN (default locale) is unprefixed, FR lives under `/fr` - the equivalent of
  the old frontend's `useLocalePath()`.
  """

  @locales ~w(en fr)

  def locales, do: @locales

  @doc """
  Prefixes `path` for the given locale.

      iex> CharsetWeb.Locale.localized_path("en", "/sandbox")
      "/sandbox"

      iex> CharsetWeb.Locale.localized_path("fr", "/sandbox")
      "/fr/sandbox"

      iex> CharsetWeb.Locale.localized_path("fr", "/")
      "/fr"
  """
  def localized_path("en", path), do: path
  def localized_path("fr", "/"), do: "/fr"
  def localized_path("fr", path), do: "/fr" <> path
end
