defmodule Charset.Exercise.Generator.Utf32ExerciseGeneratorTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.Generator.Utf32ExerciseGenerator
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  test "the exercise never asks the learner to pick the endianness (it's given in the header)" do
    refute 0xE9
           |> Utf32ExerciseGenerator.exercise_encode_steps(:utf32be)
           |> Enum.any?(&match?(%Step.Endianness{}, &1))

    refute <<0x00, 0x00, 0x00, 0xE9>>
           |> Utf32ExerciseGenerator.exercise_decode_steps(0xE9, :utf32be)
           |> Enum.any?(&match?(%Step.Endianness{}, &1))
  end

  test "encode keeps Binary + HexBytes" do
    assert [
             %Step.Binary{length: 32},
             %Step.HexBytes{expected: [0x00, 0x00, 0x00, 0xE9]}
           ] = Utf32ExerciseGenerator.exercise_encode_steps(0xE9, :utf32be)
  end

  test "decode keeps Binary + CodePointEntry" do
    assert [
             %Step.Binary{length: 32},
             %Step.CodePointEntry{expected: 0xE9}
           ] =
             Utf32ExerciseGenerator.exercise_decode_steps(
               <<0x00, 0x00, 0x00, 0xE9>>,
               0xE9,
               :utf32be
             )
  end

  describe "generate" do
    test "carries the requested encoding and level" do
      exercise = Utf32ExerciseGenerator.generate_encode!(:utf32le, 2)

      assert exercise.direction == :encode
      assert exercise.encoding == :utf32le
      assert exercise.level == 2
    end

    test "decode bytes roundtrip to the code point" do
      exercise = Utf32ExerciseGenerator.generate_decode!(:utf32le, 1)
      assert Charset.Encoding.Codec.decode!(exercise.bytes, :utf32le) == exercise.code_point
    end

    test "invalid level raises GenerationError" do
      error =
        assert_raise GenerationError, fn ->
          Utf32ExerciseGenerator.generate_decode!(:utf32le, 0)
        end

      assert Exception.message(error) ==
               "Cannot generate exercise for utf-32le level 0: level must be one of: 1, 2"
    end
  end
end
