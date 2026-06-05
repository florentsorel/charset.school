defmodule Charset.Encoding.Codec do
  @moduledoc """
  Encodes a single code point to bytes and decodes bytes back to a single
  code point, for the 8 supported encodings.

  The byte-level structure of each encoding is expressed directly as
  bitstring patterns - the UTF-8 markers (`0`, `110`, `1110`, `11110`, `10`),
  the UTF-16 surrogate prefixes (`110110`, `110111`) and the endianness all
  live in the binary syntax, not in masks and shifts.

  Dual API, stdlib-style:

    * `encode/2` / `decode/2` return `{:ok, result}` or
      `{:error, %EncodeError{} | %DecodeError{}}` with a pattern-matchable
      `reason` - for call sites where invalid input is expected (sandbox)
    * `encode!/2` / `decode!/2` return the bare result or raise - for call
      sites where invalid input is a bug (generators)

  ## Examples

      iex> Codec.encode!(0xE9, :utf8)
      <<0xC3, 0xA9>>

      iex> Codec.encode!(0x1F389, :utf8)
      <<0xF0, 0x9F, 0x8E, 0x89>>

      iex> Codec.decode!(<<0xD8, 0x3D, 0xDE, 0x00>>, :utf16be)
      0x1F600

      iex> Codec.encode(0xD800, :utf8)
      {:error, %Charset.Encoding.EncodeError{code_point: 0xD800, encoding: :utf8, reason: :surrogate}}
  """

  import Charset.Encoding.CodePoint,
    only: [is_code_point: 1, is_surrogate: 1, is_bmp: 1]

  alias Charset.Encoding
  alias Charset.Encoding.DecodeError
  alias Charset.Encoding.EncodeError
  alias Charset.Encoding.Windows1252

  @type code_point :: 0..0x10FFFF

  ## Encode

  @spec encode(code_point(), Encoding.t()) :: {:ok, binary()} | {:error, EncodeError.t()}

  # ASCII

  def encode(cp, :ascii) when is_code_point(cp) and cp <= 0x7F, do: {:ok, <<cp>>}
  def encode(cp, :ascii) when is_code_point(cp), do: encode_error(cp, :ascii, :out_of_range)

  # Latin-1

  def encode(cp, :latin1) when is_code_point(cp) and cp <= 0xFF, do: {:ok, <<cp>>}
  def encode(cp, :latin1) when is_code_point(cp), do: encode_error(cp, :latin1, :out_of_range)

  # Windows-1252: identity with Latin-1 outside the 0x80..0x9F special range,
  # 27 special mappings inside it.

  def encode(cp, :windows1252) when is_code_point(cp) and (cp <= 0x7F or cp in 0xA0..0xFF),
    do: {:ok, <<cp>>}

  def encode(cp, :windows1252) when is_code_point(cp) do
    case Windows1252.to_byte(cp) do
      nil -> encode_error(cp, :windows1252, :not_representable)
      byte -> {:ok, <<byte>>}
    end
  end

  # UTF-8: 1 to 4 bytes, the leading byte carries the sequence length marker
  # and each continuation byte is 10xxxxxx.

  def encode(cp, :utf8) when is_surrogate(cp), do: encode_error(cp, :utf8, :surrogate)

  def encode(cp, :utf8) when is_code_point(cp) and cp <= 0x7F, do: {:ok, <<cp>>}

  def encode(cp, :utf8) when is_code_point(cp) and cp <= 0x7FF do
    <<a::5, b::6>> = <<cp::11>>
    {:ok, <<0b110::3, a::5, 0b10::2, b::6>>}
  end

  def encode(cp, :utf8) when is_code_point(cp) and cp <= 0xFFFF do
    <<a::4, b::6, c::6>> = <<cp::16>>
    {:ok, <<0b1110::4, a::4, 0b10::2, b::6, 0b10::2, c::6>>}
  end

  def encode(cp, :utf8) when is_code_point(cp) do
    <<a::3, b::6, c::6, d::6>> = <<cp::21>>
    {:ok, <<0b11110::5, a::3, 0b10::2, b::6, 0b10::2, c::6, 0b10::2, d::6>>}
  end

  # UTF-16: one 16-bit code unit for the BMP, a surrogate pair (110110xx....
  # then 110111xx....) for supplementary planes.

  def encode(cp, encoding) when encoding in [:utf16be, :utf16le] and is_surrogate(cp),
    do: encode_error(cp, encoding, :surrogate)

  def encode(cp, encoding) when encoding in [:utf16be, :utf16le] and is_bmp(cp),
    do: {:ok, code_unit(cp, Encoding.endianness(encoding))}

  def encode(cp, encoding) when encoding in [:utf16be, :utf16le] and is_code_point(cp) do
    <<high_bits::10, low_bits::10>> = <<cp - 0x10000::20>>
    <<high::16>> = <<0b110110::6, high_bits::10>>
    <<low::16>> = <<0b110111::6, low_bits::10>>
    endian = Encoding.endianness(encoding)
    {:ok, code_unit(high, endian) <> code_unit(low, endian)}
  end

  # UTF-32: the code point as a fixed 32-bit unit.

  def encode(cp, encoding) when encoding in [:utf32be, :utf32le] and is_surrogate(cp),
    do: encode_error(cp, encoding, :surrogate)

  def encode(cp, :utf32be) when is_code_point(cp), do: {:ok, <<cp::32-big>>}
  def encode(cp, :utf32le) when is_code_point(cp), do: {:ok, <<cp::32-little>>}

  @spec encode!(code_point(), Encoding.t()) :: binary()
  def encode!(cp, encoding) do
    case encode(cp, encoding) do
      {:ok, bytes} -> bytes
      {:error, error} -> raise error
    end
  end

  ## Decode

  @spec decode(binary(), Encoding.t()) :: {:ok, code_point()} | {:error, DecodeError.t()}

  # ASCII

  def decode(<<byte>>, :ascii) when byte <= 0x7F, do: {:ok, byte}
  def decode(<<_byte>> = bytes, :ascii), do: decode_error(bytes, :ascii, :high_bit_set)
  def decode(bytes, :ascii) when is_binary(bytes), do: decode_error(bytes, :ascii, :bad_length)

  # Latin-1

  def decode(<<byte>>, :latin1), do: {:ok, byte}
  def decode(bytes, :latin1) when is_binary(bytes), do: decode_error(bytes, :latin1, :bad_length)

  # Windows-1252

  def decode(<<byte>>, :windows1252) when byte <= 0x7F or byte >= 0xA0, do: {:ok, byte}

  def decode(<<byte>> = bytes, :windows1252) do
    case Windows1252.to_code_point(byte) do
      nil -> decode_error(bytes, :windows1252, :unassigned_byte)
      cp -> {:ok, cp}
    end
  end

  def decode(bytes, :windows1252) when is_binary(bytes),
    do: decode_error(bytes, :windows1252, :bad_length)

  # UTF-8: the four well-formed shapes match structurally; everything else
  # falls through to `utf8_error/1` for a precise diagnostic.

  def decode(<<0::1, cp::7>>, :utf8), do: {:ok, cp}

  def decode(<<0b110::3, a::5, 0b10::2, b::6>> = bytes, :utf8) do
    <<cp::11>> = <<a::5, b::6>>
    finish_utf8(cp, 0x80, bytes)
  end

  def decode(<<0b1110::4, a::4, 0b10::2, b::6, 0b10::2, c::6>> = bytes, :utf8) do
    <<cp::16>> = <<a::4, b::6, c::6>>
    finish_utf8(cp, 0x800, bytes)
  end

  def decode(<<0b11110::5, a::3, 0b10::2, b::6, 0b10::2, c::6, 0b10::2, d::6>> = bytes, :utf8) do
    <<cp::21>> = <<a::3, b::6, c::6, d::6>>
    finish_utf8(cp, 0x10000, bytes)
  end

  def decode(bytes, :utf8) when is_binary(bytes),
    do: decode_error(bytes, :utf8, utf8_error(bytes))

  # UTF-16: a single unit (2 bytes) or a surrogate pair (4 bytes).

  def decode(<<unit::16-big>> = bytes, :utf16be),
    do: decode_utf16_single(unit, bytes, :utf16be)

  def decode(<<unit::16-little>> = bytes, :utf16le),
    do: decode_utf16_single(unit, bytes, :utf16le)

  def decode(<<u1::16-big, u2::16-big>> = bytes, :utf16be),
    do: decode_utf16_pair(u1, u2, bytes, :utf16be)

  def decode(<<u1::16-little, u2::16-little>> = bytes, :utf16le),
    do: decode_utf16_pair(u1, u2, bytes, :utf16le)

  def decode(bytes, encoding) when encoding in [:utf16be, :utf16le] and is_binary(bytes),
    do: decode_error(bytes, encoding, :bad_length)

  # UTF-32

  def decode(<<value::32-big>> = bytes, :utf32be), do: finish_utf32(value, bytes, :utf32be)
  def decode(<<value::32-little>> = bytes, :utf32le), do: finish_utf32(value, bytes, :utf32le)

  def decode(bytes, encoding) when encoding in [:utf32be, :utf32le] and is_binary(bytes),
    do: decode_error(bytes, encoding, :bad_length)

  @spec decode!(binary(), Encoding.t()) :: code_point()
  def decode!(bytes, encoding) do
    case decode(bytes, encoding) do
      {:ok, cp} -> cp
      {:error, error} -> raise error
    end
  end

  ## Helpers

  defp code_unit(unit, :big), do: <<unit::16-big>>
  defp code_unit(unit, :little), do: <<unit::16-little>>

  # A structurally well-formed UTF-8 sequence must also decode to a value that
  # requires its byte count (no overlong forms), is not a surrogate, and does
  # not exceed U+10FFFF (4-byte sequences can express up to 0x1FFFFF).
  defp finish_utf8(cp, min, bytes) do
    cond do
      cp < min -> decode_error(bytes, :utf8, {:overlong, cp})
      is_surrogate(cp) -> decode_error(bytes, :utf8, {:surrogate, cp})
      cp > 0x10FFFF -> decode_error(bytes, :utf8, {:exceeds_max, cp})
      true -> {:ok, cp}
    end
  end

  # Diagnoses a malformed UTF-8 sequence, in the same order as the old Kotlin
  # decoder: leader classification first, then length, then continuations.
  defp utf8_error(<<>>), do: :empty_input

  defp utf8_error(<<first, _rest::binary>> = bytes) do
    expected = utf8_length(first)

    cond do
      first in 0x80..0xBF -> :continuation_as_leader
      first >= 0xF8 -> :invalid_leader
      byte_size(bytes) != expected -> {:bad_length, expected}
      true -> {:invalid_continuation, bad_continuation_index(bytes)}
    end
  end

  defp utf8_length(first) when first <= 0x7F, do: 1
  defp utf8_length(first) when first <= 0xDF, do: 2
  defp utf8_length(first) when first <= 0xEF, do: 3
  defp utf8_length(_first), do: 4

  # Only reached when the length matches the leader but the sequence still
  # failed to pattern-match - some continuation byte is not 10xxxxxx.
  defp bad_continuation_index(bytes) do
    bytes
    |> :binary.bin_to_list()
    |> Enum.with_index()
    |> Enum.drop(1)
    |> Enum.find_value(fn {byte, index} ->
      if Bitwise.band(byte, 0xC0) != 0x80, do: index
    end)
  end

  defp decode_utf16_single(unit, bytes, encoding) do
    cond do
      unit in 0xD800..0xDBFF -> decode_error(bytes, encoding, {:lone_high_surrogate, unit})
      unit in 0xDC00..0xDFFF -> decode_error(bytes, encoding, {:lone_low_surrogate, unit})
      true -> {:ok, unit}
    end
  end

  defp decode_utf16_pair(u1, u2, bytes, encoding) do
    cond do
      u1 in 0xD800..0xDBFF and u2 in 0xDC00..0xDFFF ->
        <<offset::20>> = <<u1 - 0xD800::10, u2 - 0xDC00::10>>
        {:ok, 0x10000 + offset}

      u1 in 0xD800..0xDBFF ->
        decode_error(bytes, encoding, {:unpaired_high_surrogate, u1, u2})

      u1 in 0xDC00..0xDFFF ->
        decode_error(bytes, encoding, {:lone_low_surrogate, u1})

      true ->
        decode_error(bytes, encoding, {:bmp_extra_bytes, u1})
    end
  end

  defp finish_utf32(value, bytes, encoding) do
    cond do
      value > 0x10FFFF -> decode_error(bytes, encoding, {:exceeds_max, value})
      is_surrogate(value) -> decode_error(bytes, encoding, {:surrogate, value})
      true -> {:ok, value}
    end
  end

  defp encode_error(cp, encoding, reason),
    do: {:error, %EncodeError{code_point: cp, encoding: encoding, reason: reason}}

  defp decode_error(bytes, encoding, reason),
    do: {:error, %DecodeError{bytes: bytes, encoding: encoding, reason: reason}}
end
