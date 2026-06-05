defmodule Charset.Exercise.ExerciseModuleTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.ExerciseModule

  doctest Charset.Exercise.ExerciseModule

  test "exposes the 10 playable modules" do
    assert length(ExerciseModule.all()) == 10
  end

  test "ids roundtrip through from_id/1" do
    for module <- ExerciseModule.all() do
      assert module |> ExerciseModule.id() |> ExerciseModule.from_id() == module
    end
  end

  test "from_id/1 returns nil for unknown ids" do
    assert ExerciseModule.from_id("mojibake-identify") == nil
    assert ExerciseModule.from_id("") == nil
  end

  test "directions and max levels mirror the per-encoding tier sets" do
    assert ExerciseModule.direction(:utf8_encode) == :encode
    assert ExerciseModule.direction(:utf8_decode) == :decode
    assert ExerciseModule.max_level(:utf8_encode) == 4
    assert ExerciseModule.max_level(:utf16_decode) == 2
    assert ExerciseModule.max_level(:latin1_encode) == 2
    assert ExerciseModule.max_level(:windows1252_decode) == 2
  end
end
