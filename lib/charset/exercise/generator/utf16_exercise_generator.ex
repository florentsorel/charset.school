defmodule Charset.Exercise.Generator.Utf16ExerciseGenerator do
  @moduledoc """
  UTF-16 exercises (BE or LE), adapting the shared `Utf16Steps` builders:

  * the endianness step is dropped - the exercise gives the target byte order
    in its header (a random BE/LE), it isn't something the learner derives.
    Byte order is still tested via the hex-bytes step. The sandbox keeps the
    endianness step for its explanation panel.
  * BMP exercises drop the binary step - a BMP code point is a direct copy
    (the 16-bit code unit IS the scalar value), so the binary step is just a
    hex<->binary detour. The surrogate case keeps binary + bit groups, where
    the real packing happens.
  * supplementary exercises get an explicit "subtract/add 0x10000" offset
    step (entered in hex), so the conversion is its own step instead of being
    folded into the binary or code-point step.
  """

  alias Charset.Encoding
  alias Charset.Encoding.Codec
  alias Charset.Exercise
  alias Charset.Exercise.Generator.CodePointGenerator
  alias Charset.Exercise.Generator.Levels
  alias Charset.Exercise.Generator.Utf16Steps
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  @spec generate_encode!(:utf16be | :utf16le, pos_integer()) :: Exercise.t()
  def generate_encode!(encoding, level) when encoding in [:utf16be, :utf16le] do
    tier = parse_level!(encoding, level)
    code_point = CodePointGenerator.random_utf16(tier)

    Exercise.encode(code_point, encoding, level, exercise_encode_steps(code_point, encoding))
  end

  @spec generate_decode!(:utf16be | :utf16le, pos_integer()) :: Exercise.t()
  def generate_decode!(encoding, level) when encoding in [:utf16be, :utf16le] do
    tier = parse_level!(encoding, level)
    bytes = Codec.encode!(CodePointGenerator.random_utf16(tier), encoding)
    code_point = Codec.decode!(bytes, encoding)

    Exercise.decode(
      bytes,
      code_point,
      encoding,
      level,
      exercise_decode_steps(bytes, code_point, encoding)
    )
  end

  @doc "Deterministic exercise-flow step builder (exposed for tests)."
  @spec exercise_encode_steps(integer(), :utf16be | :utf16le) :: [Step.t()]
  def exercise_encode_steps(code_point, encoding) do
    code_point
    |> Utf16Steps.build_encode_steps_for(Encoding.endianness(encoding))
    |> for_exercise()
    |> with_encode_offset_step(code_point)
  end

  @doc "Deterministic exercise-flow step builder (exposed for tests)."
  @spec exercise_decode_steps(binary(), integer(), :utf16be | :utf16le) :: [Step.t()]
  def exercise_decode_steps(bytes, code_point, encoding) do
    bytes
    |> Utf16Steps.build_decode_steps_for(code_point, Encoding.endianness(encoding))
    |> for_exercise()
    |> with_decode_offset_step(code_point)
  end

  defp for_exercise(steps), do: steps |> without_endianness() |> simplify_bmp()

  defp without_endianness(steps) do
    Enum.reject(steps, &match?(%Step.Endianness{}, &1))
  end

  # A BMP exercise (no bit-groups step) goes straight from the format choice
  # to the hex bytes / code point.
  defp simplify_bmp(steps) do
    if Enum.any?(steps, &match?(%Step.BitGroups{}, &1)) do
      steps
    else
      Enum.reject(steps, &match?(%Step.Binary{}, &1))
    end
  end

  # Supplementary encode: the offset step (code point - 0x10000, in hex) goes
  # right after the format choice. BMP is left untouched.
  defp with_encode_offset_step(steps, code_point) do
    insert_offset_step(steps, code_point, Step.Format)
  end

  # Decode mirror: once the 20-bit scalar is assembled (binary step), the
  # offset step holds that value, and the code-point step adds 0x10000.
  defp with_decode_offset_step(steps, code_point) do
    insert_offset_step(steps, code_point, Step.Binary)
  end

  defp insert_offset_step(steps, code_point, after_struct) do
    offset = Utf16Steps.supplementary_offset()

    if code_point < offset do
      steps
    else
      index = Enum.find_index(steps, &is_struct(&1, after_struct)) + 1
      List.insert_at(steps, index, Step.Offset.new!(code_point - offset))
    end
  end

  defp parse_level!(encoding, level) do
    case Levels.parse(:utf16, level) do
      {:ok, tier} ->
        tier

      :error ->
        raise GenerationError,
          encoding: encoding,
          level: level,
          reason: "level must be one of: #{Levels.valid_numbers(:utf16)}"
    end
  end
end
