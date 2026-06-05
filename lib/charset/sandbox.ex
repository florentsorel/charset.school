defmodule Charset.Sandbox do
  @moduledoc """
  The sandbox: stateless step-by-step decomposition of a single conversion.

  The user types a character/code point (encode) or a byte sequence (decode);
  the steps come back fully revealed - the sandbox visualises the encoding
  mechanics, it does not quiz (nothing is validated, counted or persisted).

  Input parsing lives in `Charset.Sandbox.InputParser` / `BytesParser` /
  `EndianParser`; the step composition is delegated to the per-encoding
  builders shared with the exercise generators.
  """

  alias Charset.Exercise.Generator.Latin1Generator
  alias Charset.Exercise.Generator.Utf16Steps
  alias Charset.Exercise.Generator.Utf32Steps
  alias Charset.Exercise.Generator.Utf8Generator
  alias Charset.Exercise.Generator.Windows1252Generator
  alias Charset.Exercise.Step

  @type code_point :: 0..0x10FFFF
  @type endian :: Charset.Encoding.endian()

  @spec encode_utf8(code_point()) :: [Step.t()]
  def encode_utf8(code_point), do: Utf8Generator.build_encode_steps_for(code_point)

  @spec decode_utf8(binary(), code_point()) :: [Step.t()]
  def decode_utf8(bytes, code_point), do: Utf8Generator.build_decode_steps_for(bytes, code_point)

  @spec encode_utf16(code_point(), endian()) :: [Step.t()]
  def encode_utf16(code_point, endian), do: Utf16Steps.build_encode_steps_for(code_point, endian)

  @spec decode_utf16(binary(), code_point(), endian()) :: [Step.t()]
  def decode_utf16(bytes, code_point, endian),
    do: Utf16Steps.build_decode_steps_for(bytes, code_point, endian)

  @spec encode_utf32(code_point(), endian()) :: [Step.t()]
  def encode_utf32(code_point, endian), do: Utf32Steps.build_encode_steps_for(code_point, endian)

  @spec decode_utf32(binary(), code_point(), endian()) :: [Step.t()]
  def decode_utf32(bytes, code_point, endian),
    do: Utf32Steps.build_decode_steps_for(bytes, code_point, endian)

  @spec encode_windows1252(code_point()) :: [Step.t()]
  def encode_windows1252(code_point), do: Windows1252Generator.build_encode_steps_for(code_point)

  @spec decode_windows1252(binary(), code_point()) :: [Step.t()]
  def decode_windows1252(bytes, code_point),
    do: Windows1252Generator.build_decode_steps_for(bytes, code_point)

  @spec encode_latin1(code_point()) :: [Step.t()]
  def encode_latin1(code_point), do: Latin1Generator.build_encode_steps_for(code_point)

  @spec decode_latin1(binary()) :: [Step.t()]
  def decode_latin1(bytes), do: Latin1Generator.build_decode_steps_for(bytes)
end
