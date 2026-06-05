defmodule Charset.Encoding.CodePoint do
  @moduledoc """
  Unicode code point guards and formatting.

  A code point is a plain integer in `0..0x10FFFF` - no wrapper struct, so the
  guards compose directly into function heads (and feed the compiler's type
  inference). Values inside the surrogate range U+D800..U+DFFF are still legal
  code points (they exist in the Unicode standard as reserved values), but
  they are not valid characters and are rejected by every encoder - that
  constraint lives in the codec.
  """

  @min 0x0000
  @max 0x10FFFF
  @surrogate_min 0xD800
  @surrogate_max 0xDFFF
  # Basic Multilingual Plane
  @bmp_max 0xFFFF

  defguard is_code_point(cp) when is_integer(cp) and cp in @min..@max

  defguard is_surrogate(cp) when is_integer(cp) and cp in @surrogate_min..@surrogate_max

  defguard is_bmp(cp) when is_integer(cp) and cp in @min..@bmp_max

  def min, do: @min
  def max, do: @max
  def surrogate_min, do: @surrogate_min
  def surrogate_max, do: @surrogate_max
  def bmp_max, do: @bmp_max

  @doc """
  Formats a code point in the standard `U+XXXX` notation (4 hex digits
  minimum, more when needed).

      iex> Charset.Encoding.CodePoint.format(0xE9)
      "U+00E9"

      iex> Charset.Encoding.CodePoint.format(0x1F389)
      "U+1F389"
  """
  @spec format(integer()) :: String.t()
  def format(cp) when is_code_point(cp) do
    "U+" <> String.pad_leading(Integer.to_string(cp, 16), 4, "0")
  end
end
