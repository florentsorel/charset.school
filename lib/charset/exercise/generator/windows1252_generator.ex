defmodule Charset.Exercise.Generator.Windows1252Generator do
  @moduledoc """
  Windows-1252 exercises: single byte; identity on 0x00..0x7F and 0xA0..0xFF,
  table lookup for the 27 special characters in 0x80..0x9F.

  `build_encode_steps_for/1` and `build_decode_steps_for/2` are also used by
  the sandbox. For decode the caller decodes via `Codec.decode` first, so
  unassigned bytes (0x81, 0x8D, 0x8F, 0x90, 0x9D) surface their error early -
  the step builder only sees decodable inputs.
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
    code_point = CodePointGenerator.random_windows1252(tier)
    Exercise.encode(code_point, :windows1252, level, build_encode_steps_for(code_point))
  end

  @spec generate_decode!(pos_integer()) :: Exercise.t()
  def generate_decode!(level) do
    tier = parse_level!(level)
    bytes = Codec.encode!(CodePointGenerator.random_windows1252(tier), :windows1252)
    code_point = Codec.decode!(bytes, :windows1252)

    Exercise.decode(
      bytes,
      code_point,
      :windows1252,
      level,
      build_decode_steps_for(bytes, code_point)
    )
  end

  @spec build_encode_steps_for(integer()) :: [Step.t()]
  def build_encode_steps_for(code_point) do
    <<byte>> = Codec.encode!(code_point, :windows1252)

    [
      Step.Binary.new!(binary_string(byte, 8), 8),
      Step.HexBytes.new!([byte])
    ]
  end

  @spec build_decode_steps_for(binary(), integer()) :: [Step.t()]
  def build_decode_steps_for(<<byte>>, code_point) do
    [
      Step.Binary.new!(binary_string(byte, 8), 8),
      Step.CodePointEntry.new!(code_point)
    ]
  end

  defp binary_string(value, length) do
    value |> Integer.to_string(2) |> String.pad_leading(length, "0")
  end

  defp parse_level!(level) do
    case Levels.parse(:windows1252, level) do
      {:ok, tier} ->
        tier

      :error ->
        raise GenerationError,
          encoding: :windows1252,
          level: level,
          reason: "level must be one of: #{Levels.valid_numbers(:windows1252)}"
    end
  end
end
