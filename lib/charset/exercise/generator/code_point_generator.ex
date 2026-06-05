defmodule Charset.Exercise.Generator.CodePointGenerator do
  @moduledoc """
  Random code point picking per encoding family and tier.

  Randomness goes through the process RNG (`:rand`) - tests seed it with
  `:rand.seed/2` for determinism, or assert over distributions by sampling.
  """

  alias Charset.Encoding.CodePoint
  alias Charset.Encoding.Windows1252
  alias Charset.Exercise.Generator.Levels

  @surrogate_count CodePoint.surrogate_max() - CodePoint.surrogate_min() + 1

  @spec random_ascii(Levels.tier()) :: integer()
  def random_ascii(tier), do: :ascii |> weighted_pick(tier) |> ascii_sample()

  @spec random_latin1(Levels.tier()) :: integer()
  def random_latin1(tier), do: :latin1 |> weighted_pick(tier) |> latin1_sample()

  @spec random_windows1252(Levels.tier()) :: integer()
  def random_windows1252(tier), do: :windows1252 |> weighted_pick(tier) |> windows1252_sample()

  @spec random_utf8(Levels.tier()) :: integer()
  def random_utf8(tier), do: :utf8 |> weighted_pick(tier) |> utf8_sample()

  @spec random_utf16(Levels.tier()) :: integer()
  def random_utf16(tier), do: :utf16 |> weighted_pick(tier) |> bmp_or_supplementary_sample()

  @spec random_utf32(Levels.tier()) :: integer()
  def random_utf32(tier), do: :utf32 |> weighted_pick(tier) |> bmp_or_supplementary_sample()

  # Weighted random pick across the tier's distribution. The picked entry is
  # the sub-tier to actually sample from. Single-entry distributions (tier 1
  # = 100% of its sub-range) skip the RNG draw entirely - keeps boundary
  # tests stable and saves a draw on the hot path.
  defp weighted_pick(family, tier) do
    case Levels.distribution(family, tier) do
      [{only, _weight}] ->
        only

      distribution ->
        total = distribution |> Keyword.values() |> Enum.sum()
        roll = :rand.uniform(total) - 1

        distribution
        |> Enum.reduce_while(roll, fn {sub_tier, weight}, remaining ->
          if remaining < weight, do: {:halt, sub_tier}, else: {:cont, remaining - weight}
        end)
    end
  end

  defp ascii_sample(:printable), do: Enum.random(0x20..0x7E)
  defp ascii_sample(:full), do: Enum.random(0x00..0x7F)

  defp latin1_sample(:supplement), do: Enum.random(0xA0..0xFF)
  defp latin1_sample(:full), do: Enum.random(0x00..0xFF)

  defp windows1252_sample(:special_block), do: Enum.random(Windows1252.special_code_points())
  defp windows1252_sample(:all_encodable), do: Enum.random(Windows1252.encodable_code_points())

  # 1-byte: ASCII subset (7 data bits). 2-byte: Latin extensions, Greek,
  # Cyrillic (11 data bits). 3-byte: rest of the BMP minus surrogates.
  # 4-byte: supplementary planes (21 data bits).
  defp utf8_sample(:one_byte), do: Enum.random(0x00..0x7F)
  defp utf8_sample(:two_byte), do: Enum.random(0x80..0x7FF)
  defp utf8_sample(:three_byte), do: skipping_surrogates(0x800)
  defp utf8_sample(:four_byte), do: Enum.random(0x10000..0x10FFFF)

  defp bmp_or_supplementary_sample(:bmp), do: skipping_surrogates(0x0000)
  defp bmp_or_supplementary_sample(:supplementary), do: Enum.random(0x10000..0x10FFFF)

  # Uniform pick in start..0xFFFF excluding the surrogate block: draw an
  # index over the kept count, then shift indexes that land at or above the
  # surrogate start past the block.
  defp skipping_surrogates(start) do
    kept = CodePoint.bmp_max() - start + 1 - @surrogate_count
    index = start + :rand.uniform(kept) - 1

    if index < CodePoint.surrogate_min() do
      index
    else
      index + @surrogate_count
    end
  end
end
