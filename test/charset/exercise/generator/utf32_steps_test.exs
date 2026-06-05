defmodule Charset.Exercise.Generator.Utf32StepsTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.Generator.Utf32Steps
  alias Charset.Exercise.Step

  describe "encode" do
    test "U+00E9 big endian produces 3 steps ending in 00 00 00 E9" do
      assert [
               %Step.Endianness{expected: :big},
               %Step.Binary{expected: "00000000000000000000000011101001", length: 32},
               %Step.HexBytes{expected: [0x00, 0x00, 0x00, 0xE9]}
             ] = Utf32Steps.build_encode_steps_for(0xE9, :big)
    end

    test "U+00E9 little endian reverses the byte order to E9 00 00 00" do
      steps = Utf32Steps.build_encode_steps_for(0xE9, :little)
      assert %Step.HexBytes{expected: [0xE9, 0x00, 0x00, 0x00]} = List.last(steps)
    end

    test "U+1F389 big endian produces 3 steps ending in 00 01 F3 89" do
      assert [
               %Step.Endianness{expected: :big},
               %Step.Binary{expected: "00000000000000011111001110001001", length: 32},
               %Step.HexBytes{expected: [0x00, 0x01, 0xF3, 0x89]}
             ] = Utf32Steps.build_encode_steps_for(0x1F389, :big)
    end

    test "U+1F389 little endian reverses the byte order to 89 F3 01 00" do
      steps = Utf32Steps.build_encode_steps_for(0x1F389, :little)
      assert %Step.HexBytes{expected: [0x89, 0xF3, 0x01, 0x00]} = List.last(steps)
    end

    test "U+10FFFF big endian produces 4 bytes 00 10 FF FF" do
      steps = Utf32Steps.build_encode_steps_for(0x10FFFF, :big)

      assert %Step.Binary{expected: "00000000000100001111111111111111"} = Enum.at(steps, 1)
      assert %Step.HexBytes{expected: [0x00, 0x10, 0xFF, 0xFF]} = List.last(steps)
    end
  end

  describe "decode" do
    test "00 00 00 E9 big endian produces 3 steps ending at U+00E9" do
      assert [
               %Step.Endianness{expected: :big},
               %Step.Binary{expected: "00000000000000000000000011101001", length: 32},
               %Step.CodePointEntry{expected: 0xE9}
             ] = Utf32Steps.build_decode_steps_for(<<0x00, 0x00, 0x00, 0xE9>>, 0xE9, :big)
    end

    test "little endian input is reordered to network order before deriving the binary" do
      # E9 00 00 00 (LE) = U+00E9: the displayed bits correspond to the
      # big-endian byte sequence the decoder conceptually reads.
      assert [
               %Step.Endianness{expected: :little},
               %Step.Binary{expected: "00000000000000000000000011101001"},
               %Step.CodePointEntry{expected: 0xE9}
             ] = Utf32Steps.build_decode_steps_for(<<0xE9, 0x00, 0x00, 0x00>>, 0xE9, :little)
    end
  end
end
