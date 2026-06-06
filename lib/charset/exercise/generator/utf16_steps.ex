defmodule Charset.Exercise.Generator.Utf16Steps do
  @moduledoc """
  Shared UTF-16 step builders (the old `Utf16Generator`), used by both the
  sandbox (which keeps the endianness step for its explanation panel) and
  the exercise generator (which strips/adapts them - see
  `Utf16ExerciseGenerator`).
  """

  alias Charset.Encoding.Codec
  alias Charset.Exercise.FormatChoice
  alias Charset.Exercise.Step

  @bmp_data_bits 16
  @supplementary_data_bits 20
  @supplementary_offset 0x10000

  @spec build_encode_steps_for(integer(), Charset.Encoding.endian()) :: [Step.t()]
  def build_encode_steps_for(code_point, endian) do
    bytes = Codec.encode!(code_point, utf16_encoding(endian))
    code_unit_count = div(byte_size(bytes), 2)

    endian_step = Step.Endianness.new!(endian)
    format_step = format_step(code_unit_count)
    hex_step = Step.HexBytes.new!(:binary.bin_to_list(bytes))

    if code_unit_count == 1 do
      binary = binary_string(code_point, @bmp_data_bits)
      [endian_step, format_step, Step.Binary.new!(binary, @bmp_data_bits), hex_step]
    else
      binary = binary_string(code_point - @supplementary_offset, @supplementary_data_bits)

      [
        endian_step,
        format_step,
        Step.Binary.new!(binary, @supplementary_data_bits),
        Step.BitGroups.new!(split_surrogate_bits(binary)),
        hex_step
      ]
    end
  end

  @spec build_decode_steps_for(binary(), integer(), Charset.Encoding.endian()) :: [Step.t()]
  def build_decode_steps_for(bytes, code_point, endian) do
    code_unit_count = div(byte_size(bytes), 2)

    endian_step = Step.Endianness.new!(endian)
    format_step = format_step(code_unit_count)
    code_point_step = Step.CodePointEntry.new!(code_point)

    if code_unit_count == 1 do
      binary = binary_string(code_point, @bmp_data_bits)
      [endian_step, format_step, Step.Binary.new!(binary, @bmp_data_bits), code_point_step]
    else
      binary = binary_string(code_point - @supplementary_offset, @supplementary_data_bits)

      [
        endian_step,
        format_step,
        Step.BitGroups.new!(split_surrogate_bits(binary)),
        Step.Binary.new!(binary, @supplementary_data_bits),
        code_point_step
      ]
    end
  end

  @doc "The offset subtracted before splitting a supplementary code point."
  def supplementary_offset, do: @supplementary_offset

  defp format_step(code_unit_count) do
    choices = FormatChoice.code_unit_choices()
    Step.Format.new!(choices, Enum.at(choices, code_unit_count - 1))
  end

  defp split_surrogate_bits(binary) do
    {high, low} = String.split_at(binary, 10)
    [high, low]
  end

  defp binary_string(value, length) do
    value |> Integer.to_string(2) |> String.pad_leading(length, "0")
  end

  defp utf16_encoding(:big), do: :utf16be
  defp utf16_encoding(:little), do: :utf16le
end
