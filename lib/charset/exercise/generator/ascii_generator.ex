defmodule Charset.Exercise.Generator.AsciiGenerator do
  @moduledoc """
  ASCII exercises: single byte, identity between byte value and code point.
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
    code_point = CodePointGenerator.random_ascii(tier)
    Exercise.encode(code_point, :ascii, level, encode_steps(code_point))
  end

  @spec generate_decode!(pos_integer()) :: Exercise.t()
  def generate_decode!(level) do
    tier = parse_level!(level)
    bytes = Codec.encode!(CodePointGenerator.random_ascii(tier), :ascii)
    code_point = Codec.decode!(bytes, :ascii)
    Exercise.decode(bytes, code_point, :ascii, level, decode_steps(bytes))
  end

  @doc "Deterministic step builder for a given code point (exposed for tests)."
  @spec encode_steps(integer()) :: [Step.t()]
  def encode_steps(code_point) do
    <<byte>> = Codec.encode!(code_point, :ascii)

    [
      Step.Binary.new!(binary_string(byte, 8), 8),
      Step.HexBytes.new!([byte])
    ]
  end

  # ASCII decode is identity on the single byte: the byte's unsigned value IS
  # the code point (and its binary IS the byte's bit pattern).
  @doc "Deterministic step builder for a given byte (exposed for tests)."
  @spec decode_steps(binary()) :: [Step.t()]
  def decode_steps(<<byte>>) do
    [
      Step.Binary.new!(binary_string(byte, 8), 8),
      Step.CodePointEntry.new!(byte)
    ]
  end

  defp binary_string(value, length) do
    value |> Integer.to_string(2) |> String.pad_leading(length, "0")
  end

  defp parse_level!(level) do
    case Levels.parse(:ascii, level) do
      {:ok, tier} ->
        tier

      :error ->
        raise GenerationError,
          encoding: :ascii,
          level: level,
          reason: "level must be one of: #{Levels.valid_numbers(:ascii)}"
    end
  end
end
