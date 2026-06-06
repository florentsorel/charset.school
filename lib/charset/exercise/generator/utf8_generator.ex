defmodule Charset.Exercise.Generator.Utf8Generator do
  @moduledoc """
  UTF-8 exercises and sandbox step builders.

  EXERCISE flow: byte-aligned padded binary + explicit UsefulBitCount step,
  so the user explicitly thinks "I padded to a byte multiple, only N bits are
  useful, split them into MSB/LSB packets per UTF-8 byte".

  SANDBOX flow (`build_encode_steps_for/1`, `build_decode_steps_for/2`):
  historical layout - useful-bit binary (no padding) and no UsefulBitCount
  step, since the sandbox visualises the encoding rather than asking the
  user pedagogical questions.
  """

  alias Charset.Encoding.Codec
  alias Charset.Exercise
  alias Charset.Exercise.FormatChoice
  alias Charset.Exercise.Generator.CodePointGenerator
  alias Charset.Exercise.Generator.Levels
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  @spec generate_encode!(pos_integer()) :: Exercise.t()
  def generate_encode!(level) do
    tier = parse_level!(level)
    code_point = CodePointGenerator.random_utf8(tier)
    Exercise.encode(code_point, :utf8, level, exercise_encode_steps(code_point))
  end

  @spec generate_decode!(pos_integer()) :: Exercise.t()
  def generate_decode!(level) do
    tier = parse_level!(level)
    bytes = Codec.encode!(CodePointGenerator.random_utf8(tier), :utf8)
    code_point = Codec.decode!(bytes, :utf8)
    Exercise.decode(bytes, code_point, :utf8, level, exercise_decode_steps(bytes, code_point))
  end

  @spec build_encode_steps_for(integer()) :: [Step.t()]
  def build_encode_steps_for(code_point), do: sandbox_encode_steps(code_point)

  @spec build_decode_steps_for(binary(), integer()) :: [Step.t()]
  def build_decode_steps_for(bytes, code_point), do: sandbox_decode_steps(bytes, code_point)

  ## Exercise flow

  @doc "Deterministic exercise-flow step builder (exposed for tests)."
  @spec exercise_encode_steps(integer()) :: [Step.t()]
  def exercise_encode_steps(code_point) do
    bytes = Codec.encode!(code_point, :utf8)
    byte_count = byte_size(bytes)
    format_step = format_step(byte_count)
    hex_step = hex_step(bytes)

    # ASCII range (1 byte): binary IS the byte, hex IS the code point.
    # The format step alone teaches the identity-range insight.
    if byte_count == 1 do
      [format_step, hex_step]
    else
      data_bits = data_bits_for_byte_count(byte_count)
      padded_bits = padded_bit_count(data_bits)
      padded_binary = binary_string(code_point, padded_bits)
      useful_bits = String.slice(padded_binary, padded_bits - data_bits, data_bits)

      [
        format_step,
        Step.Binary.new!(padded_binary, padded_bits),
        Step.UsefulBitCount.new!(data_bits),
        Step.BitGroups.new!(split_bits(useful_bits, byte_count)),
        hex_step
      ]
    end
  end

  @doc "Deterministic exercise-flow step builder (exposed for tests)."
  @spec exercise_decode_steps(binary(), integer()) :: [Step.t()]
  def exercise_decode_steps(bytes, code_point) do
    byte_count = byte_size(bytes)
    data_bits = data_bits_for_byte_count(byte_count)
    combined_binary = binary_string(code_point, data_bits)
    format_step = format_step(byte_count)
    code_point_step = Step.CodePointEntry.new!(code_point)

    if byte_count == 1 do
      [format_step, code_point_step]
    else
      padded_bits = padded_bit_count(data_bits)
      padded_binary = String.pad_leading(combined_binary, padded_bits, "0")

      [
        format_step,
        Step.BitGroups.new!(split_bits(combined_binary, byte_count)),
        Step.UsefulBitCount.new!(data_bits),
        Step.Binary.new!(padded_binary, padded_bits),
        code_point_step
      ]
    end
  end

  ## Sandbox flow

  defp sandbox_encode_steps(code_point) do
    bytes = Codec.encode!(code_point, :utf8)
    byte_count = byte_size(bytes)
    format_step = format_step(byte_count)
    hex_step = hex_step(bytes)

    data_bits = data_bits_for_byte_count(byte_count)
    binary = binary_string(code_point, data_bits)
    binary_step = Step.Binary.new!(binary, data_bits)

    if byte_count == 1 do
      [format_step, binary_step, hex_step]
    else
      [format_step, binary_step, Step.BitGroups.new!(split_bits(binary, byte_count)), hex_step]
    end
  end

  defp sandbox_decode_steps(bytes, code_point) do
    byte_count = byte_size(bytes)
    data_bits = data_bits_for_byte_count(byte_count)
    combined_binary = binary_string(code_point, data_bits)
    format_step = format_step(byte_count)
    code_point_step = Step.CodePointEntry.new!(code_point)
    binary_step = Step.Binary.new!(combined_binary, data_bits)

    if byte_count == 1 do
      [format_step, binary_step, code_point_step]
    else
      [
        format_step,
        Step.BitGroups.new!(split_bits(combined_binary, byte_count)),
        binary_step,
        code_point_step
      ]
    end
  end

  ## Helpers

  defp format_step(byte_count) do
    choices = FormatChoice.byte_count_choices()
    Step.Format.new!(choices, Enum.at(choices, byte_count - 1))
  end

  defp hex_step(bytes), do: Step.HexBytes.new!(:binary.bin_to_list(bytes))

  defp binary_string(value, length) do
    value |> Integer.to_string(2) |> String.pad_leading(length, "0")
  end

  defp padded_bit_count(data_bits), do: div(data_bits + 7, 8) * 8

  defp data_bits_for_byte_count(1), do: 7
  defp data_bits_for_byte_count(2), do: 11
  defp data_bits_for_byte_count(3), do: 16
  defp data_bits_for_byte_count(4), do: 21

  # MSB-first packets per UTF-8 byte: leader payload first, then 6 bits per
  # continuation byte.
  defp split_bits(binary, 2), do: slices(binary, [5, 6])
  defp split_bits(binary, 3), do: slices(binary, [4, 6, 6])
  defp split_bits(binary, 4), do: slices(binary, [3, 6, 6, 6])

  defp slices(binary, lengths) do
    {groups, ""} =
      Enum.map_reduce(lengths, binary, fn length, rest ->
        String.split_at(rest, length)
      end)

    groups
  end

  defp parse_level!(level) do
    case Levels.parse(:utf8, level) do
      {:ok, tier} ->
        tier

      :error ->
        raise GenerationError,
          encoding: :utf8,
          level: level,
          reason: "level must be one of: #{Levels.valid_numbers(:utf8)}"
    end
  end
end
