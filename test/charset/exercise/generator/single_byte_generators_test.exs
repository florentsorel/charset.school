defmodule Charset.Exercise.Generator.SingleByteGeneratorsTest do
  @moduledoc """
  ASCII, Latin-1 and Windows-1252 exercises share the same two-step shape:
  Binary(8) + HexBytes on encode, Binary(8) + CodePointEntry on decode.
  Port of the Ascii/Latin1/Windows1252 generator tests from main.
  """
  use ExUnit.Case, async: true

  alias Charset.Exercise.Generator.AsciiGenerator
  alias Charset.Exercise.Generator.Latin1Generator
  alias Charset.Exercise.Generator.Windows1252Generator
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  describe "ascii" do
    test "encode U+0041 (A) -> Binary 01000001 + HexBytes [0x41]" do
      assert [
               %Step.Binary{expected: "01000001", length: 8},
               %Step.HexBytes{expected: [0x41]}
             ] = AsciiGenerator.encode_steps(0x41)
    end

    test "decode [0x41] -> Binary 01000001 + CodePointEntry 0x41 (identity)" do
      assert [
               %Step.Binary{expected: "01000001", length: 8},
               %Step.CodePointEntry{expected: 0x41}
             ] = AsciiGenerator.decode_steps(<<0x41>>)
    end

    test "generated exercises stay in the level's range" do
      for _draw <- 1..200 do
        exercise = AsciiGenerator.generate_encode!(1)
        # Level 1 = printable ASCII only
        assert exercise.code_point in 0x20..0x7E
      end

      for _draw <- 1..200 do
        assert AsciiGenerator.generate_encode!(2).code_point in 0x00..0x7F
      end
    end

    test "invalid level raises GenerationError" do
      error = assert_raise GenerationError, fn -> AsciiGenerator.generate_encode!(3) end

      assert Exception.message(error) ==
               "Cannot generate exercise for ascii level 3: level must be one of: 1, 2"
    end
  end

  describe "latin1" do
    test "encode U+00E9 (é) -> Binary 11101001 + HexBytes [0xE9]" do
      assert [
               %Step.Binary{expected: "11101001", length: 8},
               %Step.HexBytes{expected: [0xE9]}
             ] = Latin1Generator.build_encode_steps_for(0xE9)
    end

    test "decode [0xE9] -> Binary 11101001 + CodePointEntry 0xE9 (identity)" do
      assert [
               %Step.Binary{expected: "11101001", length: 8},
               %Step.CodePointEntry{expected: 0xE9}
             ] = Latin1Generator.build_decode_steps_for(<<0xE9>>)
    end

    test "generated exercises stay in the level's range" do
      for _draw <- 1..200 do
        # Level 1 = printable Latin-1 supplement only
        assert Latin1Generator.generate_encode!(1).code_point in 0xA0..0xFF
      end

      for _draw <- 1..200 do
        assert Latin1Generator.generate_decode!(2).code_point in 0x00..0xFF
      end
    end

    test "invalid level raises GenerationError" do
      error = assert_raise GenerationError, fn -> Latin1Generator.generate_decode!(99) end

      assert Exception.message(error) ==
               "Cannot generate exercise for latin1 level 99: level must be one of: 1, 2"
    end
  end

  describe "windows-1252" do
    test "encode U+20AC (€) -> Binary 10000000 + HexBytes [0x80] (special block)" do
      assert [
               %Step.Binary{expected: "10000000", length: 8},
               %Step.HexBytes{expected: [0x80]}
             ] = Windows1252Generator.build_encode_steps_for(0x20AC)
    end

    test "encode U+00E9 (é) -> Binary 11101001 + HexBytes [0xE9] (Latin-1 identity)" do
      assert [
               %Step.Binary{expected: "11101001", length: 8},
               %Step.HexBytes{expected: [0xE9]}
             ] = Windows1252Generator.build_encode_steps_for(0xE9)
    end

    test "decode [0x80] -> Binary 10000000 + CodePointEntry U+20AC (table lookup)" do
      assert [
               %Step.Binary{expected: "10000000", length: 8},
               %Step.CodePointEntry{expected: 0x20AC}
             ] = Windows1252Generator.build_decode_steps_for(<<0x80>>, 0x20AC)
    end

    test "level 1 draws only from the 27 special code points" do
      specials = Charset.Encoding.Windows1252.special_code_points()

      for _draw <- 1..200 do
        assert Windows1252Generator.generate_encode!(1).code_point in specials
      end
    end

    test "level 2 draws only encodable code points" do
      encodable = Charset.Encoding.Windows1252.encodable_code_points()

      for _draw <- 1..200 do
        assert Windows1252Generator.generate_decode!(2).code_point in encodable
      end
    end

    test "invalid level raises GenerationError" do
      error = assert_raise GenerationError, fn -> Windows1252Generator.generate_encode!(0) end

      assert Exception.message(error) ==
               "Cannot generate exercise for windows-1252 level 0: level must be one of: 1, 2"
    end
  end
end
