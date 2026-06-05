defmodule Charset.Exercise.Generator.Utf16ExerciseGeneratorTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.FormatChoice
  alias Charset.Exercise.Generator.Utf16ExerciseGenerator
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  defp types(steps), do: Enum.map(steps, &(&1.__struct__ |> Module.split() |> List.last()))

  test "the exercise never asks the learner to pick the endianness (it's given in the header)" do
    refute 0xE9
           |> Utf16ExerciseGenerator.exercise_encode_steps(:utf16be)
           |> Enum.any?(&match?(%Step.Endianness{}, &1))

    refute 0x1F389
           |> Utf16ExerciseGenerator.exercise_encode_steps(:utf16be)
           |> Enum.any?(&match?(%Step.Endianness{}, &1))
  end

  describe "encode" do
    test "BMP is a direct copy - no binary step, straight from format to hex bytes" do
      steps = Utf16ExerciseGenerator.exercise_encode_steps(0xE9, :utf16be)

      assert types(steps) == ["Format", "HexBytes"]
      assert %Step.Format{expected: expected} = Enum.at(steps, 0)
      assert expected == FormatChoice.one_code_unit()
      assert %Step.HexBytes{expected: [0x00, 0xE9]} = Enum.at(steps, 1)
    end

    test "supplementary inserts an offset step, then keeps binary + bit-groups (surrogate pair)" do
      steps = Utf16ExerciseGenerator.exercise_encode_steps(0x1F389, :utf16be)

      assert types(steps) == ["Format", "Offset", "Binary", "BitGroups", "HexBytes"]
      assert %Step.Format{expected: expected} = Enum.at(steps, 0)
      assert expected == FormatChoice.two_code_units()
      # 0x1F389 - 0x10000 = 0xF389
      assert %Step.Offset{expected: 0xF389} = Enum.at(steps, 1)
    end
  end

  describe "decode" do
    test "BMP reads straight from format to code point - no binary step" do
      steps = Utf16ExerciseGenerator.exercise_decode_steps(<<0x00, 0xE9>>, 0xE9, :utf16be)

      assert types(steps) == ["Format", "CodePointEntry"]
      assert %Step.CodePointEntry{expected: 0xE9} = Enum.at(steps, 1)
    end

    test "supplementary inserts an offset step after binary, before the code point" do
      steps =
        Utf16ExerciseGenerator.exercise_decode_steps(
          <<0xD8, 0x3C, 0xDF, 0x89>>,
          0x1F389,
          :utf16be
        )

      assert types(steps) == ["Format", "BitGroups", "Binary", "Offset", "CodePointEntry"]
      assert %Step.Offset{expected: 0xF389} = Enum.at(steps, 3)
      assert %Step.CodePointEntry{expected: 0x1F389} = Enum.at(steps, 4)
    end
  end

  describe "generate" do
    test "carries the requested encoding and level" do
      exercise = Utf16ExerciseGenerator.generate_encode!(:utf16le, 1)

      assert exercise.direction == :encode
      assert exercise.encoding == :utf16le
      assert exercise.level == 1
    end

    test "decode bytes roundtrip to the code point" do
      exercise = Utf16ExerciseGenerator.generate_decode!(:utf16be, 2)
      assert Charset.Encoding.Codec.decode!(exercise.bytes, :utf16be) == exercise.code_point
    end

    test "invalid level raises GenerationError" do
      error =
        assert_raise GenerationError, fn ->
          Utf16ExerciseGenerator.generate_encode!(:utf16be, 9)
        end

      assert Exception.message(error) ==
               "Cannot generate exercise for utf-16be level 9: level must be one of: 1, 2"
    end
  end
end
