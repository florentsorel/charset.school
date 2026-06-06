defmodule Charset.Exercise.Generator.Utf8GeneratorTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.FormatChoice
  alias Charset.Exercise.Generator.Utf8Generator
  alias Charset.Exercise.GenerationError
  alias Charset.Exercise.Step

  @format_choices [
    "format-choice.byte-count.1",
    "format-choice.byte-count.2",
    "format-choice.byte-count.3",
    "format-choice.byte-count.4"
  ]

  describe "exercise encode steps - 1 byte" do
    # ASCII range: the byte IS the code point, binary-to-hex would be
    # mechanical busy-work. We collapse to [Format, HexBytes] so the pedagogy
    # stays in the Format step (identity range recognition).
    test "U+0041 (A) -> Format + HexBytes (no Binary, no BitGroups)" do
      assert [%Step.Format{} = format, %Step.HexBytes{} = hex] =
               Utf8Generator.exercise_encode_steps(0x41)

      assert format.choices == @format_choices
      assert format.expected == FormatChoice.one_byte()
      assert hex.expected == [0x41]
    end

    test "U+0000 (NUL, low boundary) -> byte 0x00" do
      assert [_format, %Step.HexBytes{expected: [0x00]}] =
               Utf8Generator.exercise_encode_steps(0x00)
    end

    test "U+007F (DEL, high boundary) -> byte 0x7F" do
      assert [_format, %Step.HexBytes{expected: [0x7F]}] =
               Utf8Generator.exercise_encode_steps(0x7F)
    end
  end

  describe "exercise encode steps - 2 bytes" do
    # U+00E9 (é) -> bytes C3 A9. Binary padded to 16 bits = 0x00E9, useful
    # bits = 11 (last 11 of the padded), split 00011 / 101001.
    test "U+00E9 (é, canary) -> Format + Binary(16, padded) + UsefulBitCount(11) + BitGroups(5,6) + HexBytes" do
      assert [
               %Step.Format{} = format,
               %Step.Binary{} = binary,
               %Step.UsefulBitCount{} = useful,
               %Step.BitGroups{} = bit_groups,
               %Step.HexBytes{} = hex
             ] = Utf8Generator.exercise_encode_steps(0xE9)

      assert format.expected == FormatChoice.two_bytes()
      assert binary.length == 16
      assert binary.expected == "0000000011101001"
      assert useful.expected == 11
      assert bit_groups.expected == ["00011", "101001"]
      assert hex.expected == [0xC3, 0xA9]
    end

    test "U+0080 (low boundary) -> Binary 0000000010000000, BitGroups(00010, 000000), bytes C2 80" do
      steps = Utf8Generator.exercise_encode_steps(0x80)

      assert %Step.Binary{expected: "0000000010000000"} = Enum.at(steps, 1)
      assert %Step.UsefulBitCount{expected: 11} = Enum.at(steps, 2)
      assert %Step.BitGroups{expected: ["00010", "000000"]} = Enum.at(steps, 3)
      assert %Step.HexBytes{expected: [0xC2, 0x80]} = Enum.at(steps, 4)
    end

    test "U+07FF (high boundary) -> Binary 0000011111111111, BitGroups(11111, 111111), bytes DF BF" do
      steps = Utf8Generator.exercise_encode_steps(0x7FF)

      assert %Step.Binary{expected: "0000011111111111"} = Enum.at(steps, 1)
      assert %Step.UsefulBitCount{expected: 11} = Enum.at(steps, 2)
      assert %Step.BitGroups{expected: ["11111", "111111"]} = Enum.at(steps, 3)
      assert %Step.HexBytes{expected: [0xDF, 0xBF]} = Enum.at(steps, 4)
    end
  end

  describe "exercise encode steps - 3 bytes" do
    # U+4E2D (中) -> bytes E4 B8 AD, binary 0100111000101101 (16 bits, already
    # byte-aligned), useful bits = 16, split 0100/111000/101101.
    test "U+4E2D (中) -> Format + Binary(16) + UsefulBitCount(16) + BitGroups(4,6,6) + HexBytes" do
      assert [
               _format,
               %Step.Binary{length: 16, expected: "0100111000101101"},
               %Step.UsefulBitCount{expected: 16},
               %Step.BitGroups{expected: ["0100", "111000", "101101"]},
               %Step.HexBytes{expected: [0xE4, 0xB8, 0xAD]}
             ] = Utf8Generator.exercise_encode_steps(0x4E2D)
    end

    test "U+0800 (low boundary) -> Binary 0000100000000000, bytes E0 A0 80" do
      steps = Utf8Generator.exercise_encode_steps(0x800)

      assert %Step.Binary{expected: "0000100000000000"} = Enum.at(steps, 1)
      assert %Step.BitGroups{expected: ["0000", "100000", "000000"]} = Enum.at(steps, 3)
      assert %Step.HexBytes{expected: [0xE0, 0xA0, 0x80]} = Enum.at(steps, 4)
    end

    test "U+FFFF (high boundary, BMP max) -> bytes EF BF BF" do
      steps = Utf8Generator.exercise_encode_steps(0xFFFF)
      assert %Step.HexBytes{expected: [0xEF, 0xBF, 0xBF]} = Enum.at(steps, 4)
    end
  end

  describe "exercise encode steps - 4 bytes" do
    # U+1F600 (emoji) -> bytes F0 9F 98 80. Binary padded to 24 bits, useful
    # bits = 21, split 000 / 011111 / 011000 / 000000.
    test "U+1F600 -> Format + Binary(24, padded) + UsefulBitCount(21) + BitGroups(3,6,6,6) + HexBytes" do
      assert [
               _format,
               %Step.Binary{length: 24, expected: "000000011111011000000000"},
               %Step.UsefulBitCount{expected: 21},
               %Step.BitGroups{expected: ["000", "011111", "011000", "000000"]},
               %Step.HexBytes{expected: [0xF0, 0x9F, 0x98, 0x80]}
             ] = Utf8Generator.exercise_encode_steps(0x1F600)
    end

    test "U+10000 (low boundary, first supplementary) -> bytes F0 90 80 80" do
      steps = Utf8Generator.exercise_encode_steps(0x10000)

      assert %Step.Binary{expected: "000000010000000000000000"} = Enum.at(steps, 1)
      assert %Step.HexBytes{expected: [0xF0, 0x90, 0x80, 0x80]} = Enum.at(steps, 4)
    end

    test "U+10FFFF (high boundary, Unicode max) -> bytes F4 8F BF BF" do
      steps = Utf8Generator.exercise_encode_steps(0x10FFFF)

      assert %Step.Binary{expected: "000100001111111111111111"} = Enum.at(steps, 1)
      assert %Step.HexBytes{expected: [0xF4, 0x8F, 0xBF, 0xBF]} = Enum.at(steps, 4)
    end
  end

  describe "exercise decode steps" do
    test "1-byte [0x41] (A) -> Format + CodePointEntry (no Binary, ASCII identity)" do
      assert [%Step.Format{} = format, %Step.CodePointEntry{} = cp] =
               Utf8Generator.exercise_decode_steps(<<0x41>>, 0x41)

      assert format.expected == FormatChoice.one_byte()
      assert cp.expected == 0x41
    end

    test "2-byte [C3 A9] (é) -> Format + BitGroups(5,6) + UsefulBitCount(11) + Binary(16, padded) + CodePointEntry" do
      assert [
               %Step.Format{expected: expected_format},
               %Step.BitGroups{expected: ["00011", "101001"]},
               %Step.UsefulBitCount{expected: 11},
               %Step.Binary{length: 16, expected: "0000000011101001"},
               %Step.CodePointEntry{expected: 0xE9}
             ] = Utf8Generator.exercise_decode_steps(<<0xC3, 0xA9>>, 0xE9)

      assert expected_format == FormatChoice.two_bytes()
    end

    test "3-byte [E4 B8 AD] (中) -> BitGroups(4,6,6) + UsefulBitCount(16) + Binary(16) + CodePointEntry" do
      assert [
               %Step.Format{expected: expected_format},
               %Step.BitGroups{expected: ["0100", "111000", "101101"]},
               %Step.UsefulBitCount{expected: 16},
               %Step.Binary{expected: "0100111000101101"},
               %Step.CodePointEntry{expected: 0x4E2D}
             ] = Utf8Generator.exercise_decode_steps(<<0xE4, 0xB8, 0xAD>>, 0x4E2D)

      assert expected_format == FormatChoice.three_bytes()
    end

    test "4-byte [F0 9F 98 80] -> BitGroups(3,6,6,6) + UsefulBitCount(21) + Binary(24, padded) + CodePointEntry" do
      assert [
               %Step.Format{expected: expected_format},
               %Step.BitGroups{expected: ["000", "011111", "011000", "000000"]},
               %Step.UsefulBitCount{expected: 21},
               %Step.Binary{expected: "000000011111011000000000"},
               %Step.CodePointEntry{expected: 0x1F600}
             ] = Utf8Generator.exercise_decode_steps(<<0xF0, 0x9F, 0x98, 0x80>>, 0x1F600)

      assert expected_format == FormatChoice.four_bytes()
    end
  end

  describe "sandbox steps" do
    # Sandbox layout: useful-bit binary (no padding), no UsefulBitCount step.
    test "encode U+00E9 -> Format + Binary(11, unpadded) + BitGroups + HexBytes" do
      assert [
               %Step.Format{},
               %Step.Binary{length: 11, expected: "00011101001"},
               %Step.BitGroups{expected: ["00011", "101001"]},
               %Step.HexBytes{expected: [0xC3, 0xA9]}
             ] = Utf8Generator.build_encode_steps_for(0xE9)
    end

    test "encode U+0041 (1 byte) -> Format + Binary(7) + HexBytes" do
      assert [
               %Step.Format{},
               %Step.Binary{length: 7, expected: "1000001"},
               %Step.HexBytes{expected: [0x41]}
             ] = Utf8Generator.build_encode_steps_for(0x41)
    end

    test "decode [C3 A9] -> Format + BitGroups + Binary(11) + CodePointEntry" do
      assert [
               %Step.Format{},
               %Step.BitGroups{expected: ["00011", "101001"]},
               %Step.Binary{length: 11, expected: "00011101001"},
               %Step.CodePointEntry{expected: 0xE9}
             ] = Utf8Generator.build_decode_steps_for(<<0xC3, 0xA9>>, 0xE9)
    end
  end

  describe "generate_encode!/1 and generate_decode!/1" do
    test "returns an exercise with the requested level and utf8 encoding" do
      exercise = Utf8Generator.generate_encode!(2)

      assert exercise.direction == :encode
      assert exercise.encoding == :utf8
      assert exercise.level == 2
      assert exercise.steps != []
    end

    test "decode carries the bytes matching the code point" do
      exercise = Utf8Generator.generate_decode!(3)

      assert exercise.direction == :decode
      assert Charset.Encoding.Codec.decode!(exercise.bytes, :utf8) == exercise.code_point
    end

    test "invalid level raises GenerationError with the valid numbers" do
      error = assert_raise GenerationError, fn -> Utf8Generator.generate_encode!(99) end

      assert error.encoding == :utf8
      assert error.level == 99

      assert Exception.message(error) ==
               "Cannot generate exercise for utf-8 level 99: level must be one of: 1, 2, 3, 4"
    end
  end
end
