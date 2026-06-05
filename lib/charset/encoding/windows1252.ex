defmodule Charset.Encoding.Windows1252 do
  @moduledoc """
  The Windows-1252 specification, used by both the codec (encode/decode) and
  the future exercise generators (input picking).

  Three disjoint ranges of encodable code points:

  1. ASCII identity: U+0000..U+007F → bytes 0x00..0x7F
  2. Special block: 27 scattered code points → bytes 0x80..0x9F
     (bytes 0x81, 0x8D, 0x8F, 0x90, 0x9D are unassigned)
  3. Latin-1 supplement identity: U+00A0..U+00FF → bytes 0xA0..0xFF

  Total encodable: 128 + 27 + 96 = 251 code points.

  The single source of truth is `@special_mappings` - an ordered list of
  `{code_point, byte}` pairs sorted by byte value (0x80, 0x82, ..., 0x9F). The
  lookup maps and the public lists are derived from it at compile time, so the
  "ordered by byte value" guarantee is explicit and resilient to refactors.
  """

  # The 27 special mappings, ordered by byte value (0x80, 0x82, ..., 0x9F).
  # Bytes 0x81, 0x8D, 0x8F, 0x90, 0x9D are skipped (unassigned in Windows-1252).
  @special_mappings [
    # € - Euro Sign
    {0x20AC, 0x80},
    # ‚ - Single Low-9 Quotation Mark
    {0x201A, 0x82},
    # ƒ - Latin Small Letter F with Hook
    {0x0192, 0x83},
    # „ - Double Low-9 Quotation Mark
    {0x201E, 0x84},
    # … - Horizontal Ellipsis
    {0x2026, 0x85},
    # † - Dagger
    {0x2020, 0x86},
    # ‡ - Double Dagger
    {0x2021, 0x87},
    # ˆ - Modifier Letter Circumflex Accent
    {0x02C6, 0x88},
    # ‰ - Per Mille Sign
    {0x2030, 0x89},
    # Š - Latin Capital Letter S with Caron
    {0x0160, 0x8A},
    # ‹ - Single Left-Pointing Angle Quotation Mark
    {0x2039, 0x8B},
    # Œ - Latin Capital Ligature OE
    {0x0152, 0x8C},
    # Ž - Latin Capital Letter Z with Caron
    {0x017D, 0x8E},
    # ' - Left Single Quotation Mark
    {0x2018, 0x91},
    # ' - Right Single Quotation Mark
    {0x2019, 0x92},
    # " - Left Double Quotation Mark
    {0x201C, 0x93},
    # " - Right Double Quotation Mark
    {0x201D, 0x94},
    # • - Bullet
    {0x2022, 0x95},
    # – - En Dash
    {0x2013, 0x96},
    # — - Em Dash
    {0x2014, 0x97},
    # ˜ - Small Tilde
    {0x02DC, 0x98},
    # ™ - Trade Mark Sign
    {0x2122, 0x99},
    # š - Latin Small Letter S with Caron
    {0x0161, 0x9A},
    # › - Single Right-Pointing Angle Quotation Mark
    {0x203A, 0x9B},
    # œ - Latin Small Ligature OE
    {0x0153, 0x9C},
    # ž - Latin Small Letter Z with Caron
    {0x017E, 0x9E},
    # Ÿ - Latin Capital Letter Y with Diaeresis
    {0x0178, 0x9F}
  ]

  @code_point_to_byte Map.new(@special_mappings)
  @byte_to_code_point Map.new(@special_mappings, fn {cp, byte} -> {byte, cp} end)
  @special_code_points Enum.map(@special_mappings, fn {cp, _byte} -> cp end)
  @encodable_code_points Enum.to_list(0x00..0x7F) ++
                           @special_code_points ++ Enum.to_list(0xA0..0xFF)

  @doc """
  Forward lookup for encoding: code point → byte, nil when the code point is
  not in the special block.

      iex> Charset.Encoding.Windows1252.to_byte(0x20AC)
      0x80

      iex> Charset.Encoding.Windows1252.to_byte(0x0080)
      nil
  """
  @spec to_byte(integer()) :: byte() | nil
  def to_byte(code_point), do: @code_point_to_byte[code_point]

  @doc """
  Reverse lookup for decoding: byte → code point, nil when the byte is one of
  the 5 unassigned bytes (0x81, 0x8D, 0x8F, 0x90, 0x9D).

      iex> Charset.Encoding.Windows1252.to_code_point(0x80)
      0x20AC

      iex> Charset.Encoding.Windows1252.to_code_point(0x81)
      nil
  """
  @spec to_code_point(byte()) :: integer() | nil
  def to_code_point(byte), do: @byte_to_code_point[byte]

  @doc """
  The 27 special code points, ordered by their byte value (0x80, 0x82, ..., 0x9F).
  Used for level 1 of the Windows-1252 exercise generator.
  """
  @spec special_code_points() :: [integer()]
  def special_code_points, do: @special_code_points

  @doc """
  All 251 encodable code points, ordered as:
  - U+0000..U+007F (128 ASCII identity entries)
  - the 27 special code points (in byte order)
  - U+00A0..U+00FF (96 Latin-1 supplement identity entries)
  """
  @spec encodable_code_points() :: [integer()]
  def encodable_code_points, do: @encodable_code_points
end
