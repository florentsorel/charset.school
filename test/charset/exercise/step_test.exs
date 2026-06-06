defmodule Charset.Exercise.StepTest do
  use ExUnit.Case, async: true

  alias Charset.Exercise.Step

  describe "Format" do
    test "valid step builds" do
      step = Step.Format.new!(["a", "b"], "a")
      assert step.choices == ["a", "b"]
      assert step.expected == "a"
    end

    test "empty choices raises" do
      assert_raise ArgumentError, fn -> Step.Format.new!([], "x") end
    end

    test "expected not in choices raises" do
      assert_raise ArgumentError, fn -> Step.Format.new!(["a", "b"], "c") end
    end
  end

  describe "Binary" do
    test "valid step builds" do
      step = Step.Binary.new!("11101001", 8)
      assert step.expected == "11101001"
      assert step.length == 8
    end

    test "zero length raises" do
      assert_raise ArgumentError, fn -> Step.Binary.new!("", 0) end
    end

    test "negative length raises" do
      assert_raise ArgumentError, fn -> Step.Binary.new!("1", -1) end
    end

    test "expected length mismatching length raises" do
      assert_raise ArgumentError, fn -> Step.Binary.new!("1010", 8) end
    end

    test "expected with non-binary character raises" do
      assert_raise ArgumentError, fn -> Step.Binary.new!("1110A001", 8) end
    end
  end

  describe "BitGroups" do
    test "valid step builds" do
      step = Step.BitGroups.new!(["00011", "101001"])
      assert step.expected == ["00011", "101001"]
    end

    test "empty list raises" do
      assert_raise ArgumentError, fn -> Step.BitGroups.new!([]) end
    end

    test "empty group raises" do
      assert_raise ArgumentError, fn -> Step.BitGroups.new!(["00011", ""]) end
    end

    test "non-binary character in any group raises" do
      assert_raise ArgumentError, fn -> Step.BitGroups.new!(["00011", "10A001"]) end
    end
  end

  describe "HexBytes" do
    test "valid step builds" do
      step = Step.HexBytes.new!([0xC3, 0xA9])
      assert step.expected == [0xC3, 0xA9]
    end

    test "empty list raises" do
      # Built dynamically: a literal [] lets the type system prove the call
      # always fails and flag it - which is exactly what this test asserts.
      empty = Enum.drop([0xC3], 1)
      assert_raise ArgumentError, fn -> Step.HexBytes.new!(empty) end
    end

    test "byte > 255 raises" do
      assert_raise ArgumentError, fn -> Step.HexBytes.new!([0xC3, 0x100]) end
    end

    test "byte < 0 raises" do
      assert_raise ArgumentError, fn -> Step.HexBytes.new!([-1]) end
    end
  end

  describe "CodePointEntry" do
    test "valid code point builds" do
      assert Step.CodePointEntry.new!(0xE9).expected == 0xE9
    end

    test "boundary U+0000 builds" do
      assert Step.CodePointEntry.new!(0x0000).expected == 0x0000
    end

    test "boundary U+10FFFF builds" do
      assert Step.CodePointEntry.new!(0x10FFFF).expected == 0x10FFFF
    end

    test "negative raises" do
      assert_raise ArgumentError, fn -> Step.CodePointEntry.new!(-1) end
    end

    test "above U+10FFFF raises" do
      assert_raise ArgumentError, fn -> Step.CodePointEntry.new!(0x110000) end
    end

    test "surrogate U+D800 raises" do
      assert_raise ArgumentError, fn -> Step.CodePointEntry.new!(0xD800) end
    end

    test "surrogate U+DFFF raises" do
      assert_raise ArgumentError, fn -> Step.CodePointEntry.new!(0xDFFF) end
    end
  end

  describe "Endianness" do
    test "big endian builds" do
      assert Step.Endianness.new!(:big).expected == :big
    end

    test "little endian builds" do
      assert Step.Endianness.new!(:little).expected == :little
    end
  end

  describe "UsefulBitCount" do
    test "valid count builds" do
      assert Step.UsefulBitCount.new!(11).expected == 11
    end

    test "lower bound 1 builds" do
      assert Step.UsefulBitCount.new!(1).expected == 1
    end

    test "upper bound 32 builds" do
      assert Step.UsefulBitCount.new!(32).expected == 32
    end

    test "zero raises" do
      assert_raise ArgumentError, fn -> Step.UsefulBitCount.new!(0) end
    end

    test "negative raises" do
      assert_raise ArgumentError, fn -> Step.UsefulBitCount.new!(-1) end
    end

    test "above 32 raises" do
      assert_raise ArgumentError, fn -> Step.UsefulBitCount.new!(33) end
    end
  end

  describe "Offset" do
    test "valid value builds" do
      assert Step.Offset.new!(0xF389).expected == 0xF389
    end

    test "lower bound 0 builds" do
      assert Step.Offset.new!(0).expected == 0
    end

    test "upper bound 0xFFFFF builds" do
      assert Step.Offset.new!(0xFFFFF).expected == 0xFFFFF
    end

    test "negative raises" do
      assert_raise ArgumentError, fn -> Step.Offset.new!(-1) end
    end

    test "above 0xFFFFF raises" do
      assert_raise ArgumentError, fn -> Step.Offset.new!(0x100000) end
    end
  end

  describe "type_id/1" do
    test "every step type has its stable id" do
      assert Step.type_id(Step.Format.new!(["a"], "a")) == "format"
      assert Step.type_id(Step.Binary.new!("1", 1)) == "binary"
      assert Step.type_id(Step.BitGroups.new!(["1"])) == "bit-groups"
      assert Step.type_id(Step.HexBytes.new!([0])) == "hex-bytes"
      assert Step.type_id(Step.CodePointEntry.new!(0xE9)) == "code-point"
      assert Step.type_id(Step.UsefulBitCount.new!(8)) == "useful-bit-count"
      assert Step.type_id(Step.Endianness.new!(:big)) == "endianness"
      assert Step.type_id(Step.Offset.new!(0)) == "offset"
    end
  end
end
