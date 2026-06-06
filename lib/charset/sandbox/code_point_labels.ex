defmodule Charset.Sandbox.CodePointLabels do
  @moduledoc """
  Human-readable labels for code points whose glyph would be invisible or
  absent (control characters, whitespace, private use area, format
  characters, combining marks, non-characters, unassigned slots, ...).

  Acts as the single source of truth for "should the UI render a glyph or a
  label?": when `lookup(cp) != nil`, the page shows the returned label
  instead of the glyph.

  Sources:

    * ISO/IEC 6429 (ECMA-48) mnemonics for C0/DEL/C1
    * "SPACE" for U+0020 (would render as a literal blank)
    * common short mnemonics for named format/whitespace chars (NBSP, ZWJ,
      BOM, ...) - see `@named`
    * category-based fallback via `:unicode_util.lookup/1` for PUA / format /
      combining marks / unassigned / ...
    * explicit handling of Unicode non-characters (U+FDD0..U+FDEF and
      U+nFFFE / U+nFFFF), reported as unassigned by the category data but
      deserving a distinct label
  """

  @c0 ~w(NUL SOH STX ETX EOT ENQ ACK BEL BS HT LF VT FF CR SO SI
         DLE DC1 DC2 DC3 DC4 NAK SYN ETB CAN EM SUB ESC FS GS RS US)

  @c1 ~w(PAD HOP BPH NBH IND NEL SSA ESA HTS HTJ VTS PLD PLU RI SS2 SS3
         DCS PU1 PU2 STS CCH MW SPA EPA SOS SGC SCI CSI ST OSC PM APC)

  # Named format / invisible / bidi chars where a short mnemonic exists in
  # Unicode literature. More precise than the category fallback.
  @named %{
    # No-break space
    0x00A0 => "NBSP",
    # Soft hyphen
    0x00AD => "SHY",
    # Combining grapheme joiner
    0x034F => "CGJ",
    # Mongolian vowel separator
    0x180E => "MVS",
    # Zero-width space
    0x200B => "ZWSP",
    # Zero-width non-joiner
    0x200C => "ZWNJ",
    # Zero-width joiner
    0x200D => "ZWJ",
    # Left-to-right mark
    0x200E => "LRM",
    # Right-to-left mark
    0x200F => "RLM",
    # Line separator
    0x2028 => "LSEP",
    # Paragraph separator
    0x2029 => "PSEP",
    0x202A => "LRE",
    0x202B => "RLE",
    0x202C => "PDF",
    0x202D => "LRO",
    0x202E => "RLO",
    # Word joiner
    0x2060 => "WJ",
    0x2066 => "LRI",
    0x2067 => "RLI",
    0x2068 => "FSI",
    0x2069 => "PDI",
    # Byte order mark / zero-width no-break space
    0xFEFF => "BOM"
  }

  @spec lookup(0..0x10FFFF) :: String.t() | nil
  def lookup(code_point) when code_point in 0x00..0x1F, do: Enum.at(@c0, code_point)
  def lookup(0x20), do: "SPACE"
  def lookup(0x7F), do: "DEL"
  def lookup(code_point) when code_point in 0x80..0x9F, do: Enum.at(@c1, code_point - 0x80)
  def lookup(code_point) when is_map_key(@named, code_point), do: @named[code_point]
  def lookup(code_point), do: category_label(code_point)

  # Unicode non-characters: U+FDD0..U+FDEF and the last two code points of
  # every plane. The category data reports these as unassigned, so check
  # them first.
  defp category_label(code_point)
       when code_point in 0xFDD0..0xFDEF
       when Bitwise.band(code_point, 0xFFFE) == 0xFFFE,
       do: "NONCHAR"

  defp category_label(code_point) do
    case :unicode_util.lookup(code_point) do
      %{category: {:other, :not_assigned}} -> "UNASSIGNED"
      %{category: {:other, :private}} -> "PUA"
      %{category: {:other, :format}} -> "FORMAT"
      %{category: {:separator, :line}} -> "LSEP"
      %{category: {:separator, :paragraph}} -> "PSEP"
      %{category: {:separator, :space}} -> "WHITESPACE"
      %{category: {:mark, :non_spacing}} -> "COMBINING"
      %{category: {:mark, :enclosing}} -> "COMBINING"
      %{category: {:mark, :spacing_combining}} -> "COMBINING"
      _other -> nil
    end
  end
end
