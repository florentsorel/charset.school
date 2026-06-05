defmodule Charset.Encoding.DecodeError do
  @moduledoc """
  Raised (or returned, in the non-bang API) when a byte sequence is not a
  valid encoding of a single code point.

  `reason` is a stable, pattern-matchable term (the sandbox maps reasons to
  i18n keys); the `message/1` string is dev-facing English for logs, mirroring
  the old Kotlin `DecoderException` wording.

  Reasons:

    * `:bad_length` - wrong byte count for the encoding
    * `{:bad_length, expected}` - UTF-8 variant, expected count derived from
      the leading byte
    * `:high_bit_set` - ASCII byte above 0x7F
    * `:unassigned_byte` - one of the 5 unassigned Windows-1252 bytes
    * `:empty_input` - empty UTF-8 input
    * `:continuation_as_leader` - UTF-8 sequence starting with 10xxxxxx
    * `:invalid_leader` - UTF-8 leading byte 0xF8..0xFF
    * `{:invalid_continuation, index}` - UTF-8 byte at `index` is not 10xxxxxx
    * `{:overlong, code_point}` - UTF-8 overlong encoding
    * `{:surrogate, code_point}` - decoded value is a surrogate (UTF-8/32)
    * `{:exceeds_max, value}` - decoded value above U+10FFFF (UTF-8/32)
    * `{:lone_high_surrogate, unit}` - UTF-16 high surrogate without a pair
    * `{:unpaired_high_surrogate, high, got}` - UTF-16 high surrogate followed
      by a non-low-surrogate unit
    * `{:lone_low_surrogate, unit}` - UTF-16 input starting with a low surrogate
    * `{:bmp_extra_bytes, unit}` - UTF-16 BMP code point given as 4 bytes
  """

  alias Charset.Encoding
  alias Charset.Encoding.Bytes
  alias Charset.Encoding.CodePoint

  @type reason ::
          :bad_length
          | {:bad_length, pos_integer()}
          | :high_bit_set
          | :unassigned_byte
          | :empty_input
          | :continuation_as_leader
          | :invalid_leader
          | {:invalid_continuation, pos_integer()}
          | {:overlong, integer()}
          | {:surrogate, integer()}
          | {:exceeds_max, integer()}
          | {:lone_high_surrogate, integer()}
          | {:unpaired_high_surrogate, integer(), integer()}
          | {:lone_low_surrogate, integer()}
          | {:bmp_extra_bytes, integer()}

  @type t :: %__MODULE__{
          bytes: binary(),
          encoding: Encoding.t(),
          reason: reason()
        }

  defexception [:bytes, :encoding, :reason]

  @impl Exception
  def message(%__MODULE__{bytes: bytes, encoding: encoding, reason: reason}) do
    "Cannot decode [#{Bytes.to_hex(bytes)}] in #{Encoding.id(encoding)}: " <>
      describe(reason, bytes, encoding)
  end

  defp describe(:bad_length, bytes, encoding) when encoding in [:ascii, :latin1, :windows1252],
    do: "expected exactly 1 byte, got #{byte_size(bytes)}"

  defp describe(:bad_length, bytes, encoding) when encoding in [:utf16be, :utf16le],
    do: "expected 2 or 4 bytes, got #{byte_size(bytes)}"

  defp describe(:bad_length, bytes, encoding) when encoding in [:utf32be, :utf32le],
    do: "expected exactly 4 bytes, got #{byte_size(bytes)}"

  defp describe({:bad_length, expected}, <<first, _rest::binary>> = bytes, _encoding),
    do: "expected #{expected} bytes for leading byte #{hex_byte(first)}, got #{byte_size(bytes)}"

  defp describe(:high_bit_set, _bytes, _encoding), do: "high bit set, not ASCII"

  defp describe(:unassigned_byte, <<byte>>, _encoding),
    do: "byte #{hex_byte(byte)} is unassigned in Windows-1252"

  defp describe(:empty_input, _bytes, _encoding), do: "empty input"

  defp describe(:continuation_as_leader, <<first, _rest::binary>>, _encoding),
    do: "byte #{hex_byte(first)} is a continuation byte, not a valid leader"

  defp describe(:invalid_leader, <<first, _rest::binary>>, _encoding),
    do: "invalid leading byte #{hex_byte(first)}"

  defp describe({:invalid_continuation, index}, bytes, _encoding),
    do: "byte #{index} (#{hex_byte(:binary.at(bytes, index))}) is not a valid continuation byte"

  defp describe({:overlong, code_point}, _bytes, _encoding),
    do: "overlong encoding: #{CodePoint.format(code_point)} should use a shorter form"

  defp describe({:surrogate, code_point}, _bytes, _encoding),
    do: "surrogate #{CodePoint.format(code_point)} not a valid code point"

  defp describe({:exceeds_max, value}, _bytes, encoding) when encoding in [:utf32be, :utf32le],
    do: "value 0x#{Integer.to_string(value, 16)} exceeds U+10FFFF"

  defp describe({:exceeds_max, value}, _bytes, _encoding),
    do: "value U+#{Integer.to_string(value, 16)} exceeds U+10FFFF"

  defp describe({:lone_high_surrogate, unit}, _bytes, _encoding),
    do: "high surrogate #{hex_unit(unit)} requires a following low surrogate"

  defp describe({:unpaired_high_surrogate, high, got}, _bytes, _encoding),
    do: "high surrogate #{hex_unit(high)} not followed by low surrogate, got #{hex_unit(got)}"

  defp describe({:lone_low_surrogate, unit}, _bytes, _encoding),
    do: "lone low surrogate #{hex_unit(unit)}"

  defp describe({:bmp_extra_bytes, unit}, bytes, _encoding),
    do: "BMP code point #{hex_unit(unit)} must be exactly 2 bytes, got #{byte_size(bytes)}"

  defp hex_byte(byte), do: "0x" <> String.pad_leading(Integer.to_string(byte, 16), 2, "0")

  defp hex_unit(unit), do: "0x" <> String.pad_leading(Integer.to_string(unit, 16), 4, "0")
end
