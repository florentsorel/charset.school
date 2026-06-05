defmodule Charset.Exercise.Generator.Latin1Generator do
  @moduledoc """
  Latin-1 exercises: single byte, identity between byte value and code point.

  `build_encode_steps_for/1` and `build_decode_steps_for/1` are also used by
  the sandbox (no level involved).
  """

  alias Charset.Encoding.Codec
  alias Charset.Exercise
  alias Charset.Exercise.Generator.CodePointGenerator
  alias Charset.Exercise.Generator.Levels
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  @spec generate_encode!(pos_integer()) :: Exercise.t()
  def generate_encode!(level) do
    tier = parse_level!(level)
    code_point = CodePointGenerator.random_latin1(tier)
    Exercise.encode(code_point, :latin1, level, build_encode_steps_for(code_point))
  end

  @spec generate_decode!(pos_integer()) :: Exercise.t()
  def generate_decode!(level) do
    tier = parse_level!(level)
    bytes = Codec.encode!(CodePointGenerator.random_latin1(tier), :latin1)
    code_point = Codec.decode!(bytes, :latin1)
    Exercise.decode(bytes, code_point, :latin1, level, build_decode_steps_for(bytes))
  end

  @spec build_encode_steps_for(integer()) :: [Step.t()]
  def build_encode_steps_for(code_point) do
    <<byte>> = Codec.encode!(code_point, :latin1)

    [
      Step.Binary.new!(binary_string(byte, 8), 8),
      Step.HexBytes.new!([byte])
    ]
  end

  @spec build_decode_steps_for(binary()) :: [Step.t()]
  def build_decode_steps_for(<<byte>>) do
    [
      Step.Binary.new!(binary_string(byte, 8), 8),
      Step.CodePointEntry.new!(byte)
    ]
  end

  defp binary_string(value, length) do
    value |> Integer.to_string(2) |> String.pad_leading(length, "0")
  end

  defp parse_level!(level) do
    case Levels.parse(:latin1, level) do
      {:ok, tier} ->
        tier

      :error ->
        raise GenerationError,
          encoding: :latin1,
          level: level,
          reason: "level must be one of: #{Levels.valid_numbers(:latin1)}"
    end
  end
end
