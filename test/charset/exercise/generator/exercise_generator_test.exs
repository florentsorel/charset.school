defmodule Charset.Exercise.Generator.ExerciseGeneratorTest do
  use ExUnit.Case, async: true

  alias Charset.Encoding.Codec
  alias Charset.Exercise.Generator.ExerciseGenerator
  alias Charset.Exercise.GenerationError

  test "routes every encoding to its generator (encode)" do
    for encoding <- Charset.Encoding.all() do
      exercise = ExerciseGenerator.generate_encode!(encoding, 1)

      assert exercise.direction == :encode
      assert exercise.encoding == encoding
      assert exercise.level == 1
      assert exercise.steps != []
    end
  end

  test "routes every encoding to its generator (decode) and bytes roundtrip" do
    for encoding <- Charset.Encoding.all() do
      exercise = ExerciseGenerator.generate_decode!(encoding, 1)

      assert exercise.direction == :decode
      assert exercise.encoding == encoding
      assert Codec.decode!(exercise.bytes, encoding) == exercise.code_point
    end
  end

  test "propagates GenerationError for invalid levels" do
    assert_raise GenerationError, fn -> ExerciseGenerator.generate_encode!(:utf8, 99) end
    assert_raise GenerationError, fn -> ExerciseGenerator.generate_decode!(:utf32le, 99) end
  end
end
