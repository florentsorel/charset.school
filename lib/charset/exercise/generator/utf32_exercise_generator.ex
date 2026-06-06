defmodule Charset.Exercise.Generator.Utf32ExerciseGenerator do
  @moduledoc """
  UTF-32 exercises (BE or LE). The endianness step is dropped - the exercise
  gives the target byte order in its header (a random BE/LE), it isn't
  something the learner derives. Byte order is still tested via the
  hex-bytes step. The sandbox keeps the endianness step (shared `Utf32Steps`)
  for its explanation panel.
  """

  alias Charset.Encoding
  alias Charset.Encoding.Codec
  alias Charset.Exercise
  alias Charset.Exercise.Generator.CodePointGenerator
  alias Charset.Exercise.Generator.Levels
  alias Charset.Exercise.Generator.Utf32Steps
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  @spec generate_encode!(:utf32be | :utf32le, pos_integer()) :: Exercise.t()
  def generate_encode!(encoding, level) when encoding in [:utf32be, :utf32le] do
    tier = parse_level!(encoding, level)
    code_point = CodePointGenerator.random_utf32(tier)

    Exercise.encode(code_point, encoding, level, exercise_encode_steps(code_point, encoding))
  end

  @spec generate_decode!(:utf32be | :utf32le, pos_integer()) :: Exercise.t()
  def generate_decode!(encoding, level) when encoding in [:utf32be, :utf32le] do
    tier = parse_level!(encoding, level)
    bytes = Codec.encode!(CodePointGenerator.random_utf32(tier), encoding)
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
  @spec exercise_encode_steps(integer(), :utf32be | :utf32le) :: [Step.t()]
  def exercise_encode_steps(code_point, encoding) do
    code_point
    |> Utf32Steps.build_encode_steps_for(Encoding.endianness(encoding))
    |> without_endianness()
  end

  @doc "Deterministic exercise-flow step builder (exposed for tests)."
  @spec exercise_decode_steps(binary(), integer(), :utf32be | :utf32le) :: [Step.t()]
  def exercise_decode_steps(bytes, code_point, encoding) do
    bytes
    |> Utf32Steps.build_decode_steps_for(code_point, Encoding.endianness(encoding))
    |> without_endianness()
  end

  defp without_endianness(steps) do
    Enum.reject(steps, &match?(%Step.Endianness{}, &1))
  end

  defp parse_level!(encoding, level) do
    case Levels.parse(:utf32, level) do
      {:ok, tier} ->
        tier

      :error ->
        raise GenerationError,
          encoding: encoding,
          level: level,
          reason: "level must be one of: #{Levels.valid_numbers(:utf32)}"
    end
  end
end
