defmodule Charset.Exercise.ExerciseModule do
  @moduledoc """
  The 10 playable exercise modules (encoding family × direction).

  `max_level` mirrors the per-encoding level tiers used by the matching
  generator. The progress layer reads it to cap auto-advance, and the UI
  renders the progression indicator against it ("Niveau X / max_level").
  Single source of truth for the family's tier count.
  """

  @modules %{
    utf8_encode: %{id: "utf8-encode", direction: :encode, max_level: 4},
    utf8_decode: %{id: "utf8-decode", direction: :decode, max_level: 4},
    utf16_encode: %{id: "utf16-encode", direction: :encode, max_level: 2},
    utf16_decode: %{id: "utf16-decode", direction: :decode, max_level: 2},
    utf32_encode: %{id: "utf32-encode", direction: :encode, max_level: 2},
    utf32_decode: %{id: "utf32-decode", direction: :decode, max_level: 2},
    latin1_encode: %{id: "latin1-encode", direction: :encode, max_level: 2},
    latin1_decode: %{id: "latin1-decode", direction: :decode, max_level: 2},
    windows1252_encode: %{id: "windows1252-encode", direction: :encode, max_level: 2},
    windows1252_decode: %{id: "windows1252-decode", direction: :decode, max_level: 2}
  }

  @type t ::
          :utf8_encode
          | :utf8_decode
          | :utf16_encode
          | :utf16_decode
          | :utf32_encode
          | :utf32_decode
          | :latin1_encode
          | :latin1_decode
          | :windows1252_encode
          | :windows1252_decode

  defguard is_module(module) when is_map_key(@modules, module)

  @spec all() :: [t()]
  def all do
    [
      :utf8_encode,
      :utf8_decode,
      :utf16_encode,
      :utf16_decode,
      :utf32_encode,
      :utf32_decode,
      :latin1_encode,
      :latin1_decode,
      :windows1252_encode,
      :windows1252_decode
    ]
  end

  @doc """
  The stable string id (wire/DB identifier).

      iex> Charset.Exercise.ExerciseModule.id(:utf16_decode)
      "utf16-decode"
  """
  @spec id(t()) :: String.t()
  def id(module) when is_module(module), do: @modules[module].id

  @spec from_id(String.t()) :: t() | nil
  def from_id(id) when is_binary(id) do
    Enum.find(all(), fn module -> @modules[module].id == id end)
  end

  @spec direction(t()) :: :encode | :decode
  def direction(module) when is_module(module), do: @modules[module].direction

  @spec max_level(t()) :: pos_integer()
  def max_level(module) when is_module(module), do: @modules[module].max_level
end
