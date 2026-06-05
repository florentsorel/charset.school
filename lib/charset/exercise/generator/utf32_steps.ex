defmodule Charset.Exercise.Generator.Utf32Steps do
  @moduledoc """
  Shared UTF-32 step builders (the old `Utf32Generator`), used by both the
  sandbox (which keeps the endianness step) and the exercise generator
  (which strips it - see `Utf32ExerciseGenerator`).
  """

  alias Charset.Encoding.Codec
  alias Charset.Exercise.Step

  @utf32_bits 32

  @spec build_encode_steps_for(integer(), Charset.Encoding.endian()) :: [Step.t()]
  def build_encode_steps_for(code_point, endian) do
    bytes = Codec.encode!(code_point, utf32_encoding(endian))

    [
      Step.Endianness.new!(endian),
      Step.Binary.new!(binary_string(code_point, @utf32_bits), @utf32_bits),
      Step.HexBytes.new!(:binary.bin_to_list(bytes))
    ]
  end

  @spec build_decode_steps_for(binary(), integer(), Charset.Encoding.endian()) :: [Step.t()]
  def build_decode_steps_for(bytes, code_point, endian) do
    # Reorder the user-facing bytes back to network order before deriving the
    # binary, so the displayed bits actually correspond to the byte sequence
    # the decoder will read.
    <<value::32>> =
      case endian do
        :big -> bytes
        :little -> bytes |> :binary.bin_to_list() |> Enum.reverse() |> :binary.list_to_bin()
      end

    [
      Step.Endianness.new!(endian),
      Step.Binary.new!(binary_string(value, @utf32_bits), @utf32_bits),
      Step.CodePointEntry.new!(code_point)
    ]
  end

  defp binary_string(value, length) do
    value |> Integer.to_string(2) |> String.pad_leading(length, "0")
  end

  defp utf32_encoding(:big), do: :utf32be
  defp utf32_encoding(:little), do: :utf32le
end
