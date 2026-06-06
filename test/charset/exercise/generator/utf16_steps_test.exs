defmodule Charset.Exercise.Generator.Utf16StepsTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.FormatChoice
  alias Charset.Exercise.Generator.Utf16Steps
  alias Charset.Exercise.Step

  describe "encode - BMP code point (U+00E9 é)" do
    test "big endian produces 4 steps ending in 00 E9" do
      assert [
               %Step.Endianness{expected: :big},
               %Step.Format{expected: format},
               %Step.Binary{expected: "0000000011101001", length: 16},
               %Step.HexBytes{expected: [0x00, 0xE9]}
             ] = Utf16Steps.build_encode_steps_for(0xE9, :big)

      assert format == FormatChoice.one_code_unit()
    end

    test "little endian swaps the byte order to E9 00" do
      steps = Utf16Steps.build_encode_steps_for(0xE9, :little)
      assert %Step.HexBytes{expected: [0xE9, 0x00]} = List.last(steps)
    end
  end

  describe "encode - supplementary code point (U+1F389 tada)" do
    test "big endian produces 5 steps with surrogate pair D8 3C DF 89" do
      # 0x1F389 - 0x10000 = 0xF389, padded to 20 bits. High surrogate =
      # 0xD800 + 0x3C = 0xD83C. Low = 0xDC00 + 0x389 = 0xDF89.
      assert [
               %Step.Endianness{expected: :big},
               %Step.Format{expected: format},
               %Step.Binary{expected: "00001111001110001001", length: 20},
               %Step.BitGroups{expected: ["0000111100", "1110001001"]},
               %Step.HexBytes{expected: [0xD8, 0x3C, 0xDF, 0x89]}
             ] = Utf16Steps.build_encode_steps_for(0x1F389, :big)

      assert format == FormatChoice.two_code_units()
    end

    test "little endian swaps each code unit to 3C D8 89 DF" do
      steps = Utf16Steps.build_encode_steps_for(0x1F389, :little)
      assert %Step.HexBytes{expected: [0x3C, 0xD8, 0x89, 0xDF]} = List.last(steps)
    end
  end

  describe "decode" do
    test "00 E9 big endian produces 4 steps ending at U+00E9" do
      assert [
               %Step.Endianness{expected: :big},
               %Step.Format{expected: format},
               %Step.Binary{expected: "0000000011101001"},
               %Step.CodePointEntry{expected: 0xE9}
             ] = Utf16Steps.build_decode_steps_for(<<0x00, 0xE9>>, 0xE9, :big)

      assert format == FormatChoice.one_code_unit()
    end

    test "D8 3C DF 89 big endian produces 5 steps ending at U+1F389" do
      assert [
               %Step.Endianness{expected: :big},
               %Step.Format{expected: format},
               %Step.BitGroups{expected: ["0000111100", "1110001001"]},
               %Step.Binary{expected: "00001111001110001001", length: 20},
               %Step.CodePointEntry{expected: 0x1F389}
             ] = Utf16Steps.build_decode_steps_for(<<0xD8, 0x3C, 0xDF, 0x89>>, 0x1F389, :big)

      assert format == FormatChoice.two_code_units()
    end
  end
end
