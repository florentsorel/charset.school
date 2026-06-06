defmodule Charset.Exercise.Generator.ExerciseGenerator do
  @moduledoc """
  Thin dispatcher: routes `generate_encode!` / `generate_decode!` calls to
  the matching per-encoding generator. Where the Kotlin version assembled a
  registry of injected generator beans, the wiring here is plain pattern
  matching on the encoding.
  """

  alias Charset.Exercise

  alias Charset.Exercise.Generator.{
    AsciiGenerator,
    Latin1Generator,
    Utf16ExerciseGenerator,
    Utf32ExerciseGenerator,
    Utf8Generator,
    Windows1252Generator
  }

  @spec generate_encode!(Charset.Encoding.t(), pos_integer()) :: Exercise.t()
  def generate_encode!(:ascii, level), do: AsciiGenerator.generate_encode!(level)
  def generate_encode!(:latin1, level), do: Latin1Generator.generate_encode!(level)
  def generate_encode!(:windows1252, level), do: Windows1252Generator.generate_encode!(level)
  def generate_encode!(:utf8, level), do: Utf8Generator.generate_encode!(level)

  def generate_encode!(encoding, level) when encoding in [:utf16be, :utf16le],
    do: Utf16ExerciseGenerator.generate_encode!(encoding, level)

  def generate_encode!(encoding, level) when encoding in [:utf32be, :utf32le],
    do: Utf32ExerciseGenerator.generate_encode!(encoding, level)

  @spec generate_decode!(Charset.Encoding.t(), pos_integer()) :: Exercise.t()
  def generate_decode!(:ascii, level), do: AsciiGenerator.generate_decode!(level)
  def generate_decode!(:latin1, level), do: Latin1Generator.generate_decode!(level)
  def generate_decode!(:windows1252, level), do: Windows1252Generator.generate_decode!(level)
  def generate_decode!(:utf8, level), do: Utf8Generator.generate_decode!(level)

  def generate_decode!(encoding, level) when encoding in [:utf16be, :utf16le],
    do: Utf16ExerciseGenerator.generate_decode!(encoding, level)

  def generate_decode!(encoding, level) when encoding in [:utf32be, :utf32le],
    do: Utf32ExerciseGenerator.generate_decode!(encoding, level)
end
