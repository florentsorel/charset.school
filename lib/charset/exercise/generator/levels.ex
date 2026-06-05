defmodule Charset.Exercise.Generator.Levels do
  @moduledoc """
  Per-encoding-family difficulty tiers and their weighted sub-range mixes.

  Each family maps a level number (what the progress layer stores) to a tier
  atom, and each tier to a weighted distribution of sub-tiers to draw from.
  Mixing (e.g. UTF-8 level 4 = 10/30/30/30) keeps lower byte-counts in
  rotation for spiral practice and prevents the Format step from being a
  trivial deterministic answer.
  """

  @tiers %{
    # U+0020..U+007E (printable), U+0000..U+007F (full)
    ascii: %{1 => :printable, 2 => :full},
    # U+00A0..U+00FF (printable supplement), U+0000..U+00FF (full)
    latin1: %{1 => :supplement, 2 => :full},
    # the 27 special code points, then all 251 encodable
    windows1252: %{1 => :special_block, 2 => :all_encodable},
    utf8: %{1 => :one_byte, 2 => :two_byte, 3 => :three_byte, 4 => :four_byte},
    # single 16-bit unit (BMP), then surrogate pairs (supplementary)
    utf16: %{1 => :bmp, 2 => :supplementary},
    utf32: %{1 => :bmp, 2 => :supplementary}
  }

  @distributions %{
    {:ascii, :printable} => [printable: 100],
    {:ascii, :full} => [printable: 40, full: 60],
    {:latin1, :supplement} => [supplement: 100],
    {:latin1, :full} => [supplement: 40, full: 60],
    {:windows1252, :special_block} => [special_block: 100],
    {:windows1252, :all_encodable} => [special_block: 30, all_encodable: 70],
    {:utf8, :one_byte} => [one_byte: 100],
    {:utf8, :two_byte} => [one_byte: 20, two_byte: 80],
    {:utf8, :three_byte} => [one_byte: 10, two_byte: 25, three_byte: 65],
    {:utf8, :four_byte} => [one_byte: 10, two_byte: 30, three_byte: 30, four_byte: 30],
    {:utf16, :bmp} => [bmp: 100],
    {:utf16, :supplementary} => [bmp: 30, supplementary: 70],
    {:utf32, :bmp} => [bmp: 100],
    {:utf32, :supplementary} => [bmp: 30, supplementary: 70]
  }

  @type family :: :ascii | :latin1 | :windows1252 | :utf8 | :utf16 | :utf32
  @type tier :: atom()

  @spec parse(family(), integer()) :: {:ok, tier()} | :error
  def parse(family, level) do
    case @tiers[family][level] do
      nil -> :error
      tier -> {:ok, tier}
    end
  end

  @doc "Weighted sub-tier mix to draw from at this tier."
  @spec distribution(family(), tier()) :: [{tier(), pos_integer()}]
  def distribution(family, tier), do: Map.fetch!(@distributions, {family, tier})

  @doc "Human-readable list of valid level numbers, for generation errors."
  @spec valid_numbers(family()) :: String.t()
  def valid_numbers(family) do
    @tiers |> Map.fetch!(family) |> Map.keys() |> Enum.sort() |> Enum.join(", ")
  end
end
