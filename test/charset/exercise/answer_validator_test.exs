defmodule Charset.Exercise.AnswerValidatorTest do
  @moduledoc """
  Port of the Kotlin `AnswerValidatorTest` - same cases, same labels. The
  only divergence: `got-type` in `answer.type-mismatch` params carries the
  stable answer id ("hex-bytes") instead of the Kotlin class name
  ("HexBytesValue").
  """
  use ExUnit.Case, async: true

  alias Charset.Exercise.AnswerValidator
  alias Charset.Exercise.ErrorType
  alias Charset.Exercise.ParamKey
  alias Charset.Exercise.Step
  alias Charset.Exercise.ValidationResult

  describe "binary" do
    setup do
      %{step: Step.Binary.new!("11101001", 8)}
    end

    test "correct value -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:binary, "11101001"}) == ValidationResult.correct()
    end

    test "empty input -> binary.empty", %{step: step} do
      assert AnswerValidator.validate(step, {:binary, ""}) ==
               ValidationResult.incorrect(ErrorType.binary_empty())
    end

    test "non-binary character -> binary.invalid-character", %{step: step} do
      assert AnswerValidator.validate(step, {:binary, "1110A001"}) ==
               ValidationResult.incorrect(ErrorType.binary_invalid_character(), %{
                 ParamKey.got() => "1110A001"
               })
    end

    test "shorter than expected -> binary.too-few-bits", %{step: step} do
      assert AnswerValidator.validate(step, {:binary, "1110100"}) ==
               ValidationResult.incorrect(ErrorType.binary_too_few_bits(), %{
                 ParamKey.expected_length() => "8",
                 ParamKey.got_length() => "7"
               })
    end

    test "longer than expected -> binary.too-many-bits", %{step: step} do
      assert AnswerValidator.validate(step, {:binary, "111010010"}) ==
               ValidationResult.incorrect(ErrorType.binary_too_many_bits(), %{
                 ParamKey.expected_length() => "8",
                 ParamKey.got_length() => "9"
               })
    end

    test "right length but wrong value -> binary.wrong-value (no expected leak)", %{step: step} do
      assert AnswerValidator.validate(step, {:binary, "00000000"}) ==
               ValidationResult.incorrect(ErrorType.binary_wrong_value(), %{
                 ParamKey.got() => "00000000"
               })
    end

    test "alphabet checked before length (mostly invalid chars, wrong length)", %{step: step} do
      result = AnswerValidator.validate(step, {:binary, "ABCD"})
      refute result.ok
      assert result.error_type == ErrorType.binary_invalid_character()
    end

    test "length checked before value (right alphabet, wrong length)", %{step: step} do
      result = AnswerValidator.validate(step, {:binary, "11111111111"})
      refute result.ok
      assert result.error_type == ErrorType.binary_too_many_bits()
    end

    test "wrong answer type -> answer.type-mismatch", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [0xE9]}) ==
               ValidationResult.incorrect(ErrorType.answer_type_mismatch(), %{
                 ParamKey.expected_type() => "binary",
                 ParamKey.got_type() => "hex-bytes"
               })
    end
  end

  describe "format" do
    setup do
      %{step: Step.Format.new!(["1 byte", "2 bytes", "3 bytes", "4 bytes"], "2 bytes")}
    end

    test "correct choice -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:format, "2 bytes"}) == ValidationResult.correct()
    end

    test "empty -> format.empty", %{step: step} do
      assert AnswerValidator.validate(step, {:format, ""}) ==
               ValidationResult.incorrect(ErrorType.format_empty())
    end

    test "choice not in list -> format.unknown-choice", %{step: step} do
      assert AnswerValidator.validate(step, {:format, "5 bytes"}) ==
               ValidationResult.incorrect(ErrorType.format_unknown_choice(), %{
                 ParamKey.got() => "5 bytes",
                 ParamKey.choices() => "1 byte, 2 bytes, 3 bytes, 4 bytes"
               })
    end

    test "wrong choice -> format.wrong-choice (no expected leak)", %{step: step} do
      assert AnswerValidator.validate(step, {:format, "3 bytes"}) ==
               ValidationResult.incorrect(ErrorType.format_wrong_choice(), %{
                 ParamKey.got() => "3 bytes"
               })
    end

    test "wrong answer type -> answer.type-mismatch", %{step: step} do
      result = AnswerValidator.validate(step, {:binary, "0"})
      assert result.error_type == ErrorType.answer_type_mismatch()
      assert result.params[ParamKey.expected_type()] == "format"
    end
  end

  describe "bit-groups" do
    setup do
      %{step: Step.BitGroups.new!(["00011", "101001"])}
    end

    test "correct value -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:bit_groups, ["00011", "101001"]}) ==
               ValidationResult.correct()
    end

    test "empty -> bit-groups.empty", %{step: step} do
      assert AnswerValidator.validate(step, {:bit_groups, []}) ==
               ValidationResult.incorrect(ErrorType.bit_groups_empty())
    end

    test "wrong group count (too few) -> bit-groups.wrong-group-count", %{step: step} do
      assert AnswerValidator.validate(step, {:bit_groups, ["00011"]}) ==
               ValidationResult.incorrect(ErrorType.bit_groups_wrong_group_count(), %{
                 ParamKey.expected_count() => "2",
                 ParamKey.got_count() => "1"
               })
    end

    test "wrong group count (too many) -> bit-groups.wrong-group-count", %{step: step} do
      assert AnswerValidator.validate(step, {:bit_groups, ["00011", "101001", "111"]}) ==
               ValidationResult.incorrect(ErrorType.bit_groups_wrong_group_count(), %{
                 ParamKey.expected_count() => "2",
                 ParamKey.got_count() => "3"
               })
    end

    test "invalid char in second group -> bit-groups.invalid-character", %{step: step} do
      assert AnswerValidator.validate(step, {:bit_groups, ["00011", "10A001"]}) ==
               ValidationResult.incorrect(ErrorType.bit_groups_invalid_character(), %{
                 ParamKey.position() => "1",
                 ParamKey.got() => "10A001"
               })
    end

    test "wrong length on first group -> bit-groups.wrong-group-length", %{step: step} do
      assert AnswerValidator.validate(step, {:bit_groups, ["0001", "101001"]}) ==
               ValidationResult.incorrect(ErrorType.bit_groups_wrong_group_length(), %{
                 ParamKey.position() => "0",
                 ParamKey.expected_length() => "5",
                 ParamKey.got_length() => "4"
               })
    end

    test "right structure but wrong bits -> bit-groups.wrong-value (no expected leak)", %{
      step: step
    } do
      assert AnswerValidator.validate(step, {:bit_groups, ["11111", "000000"]}) ==
               ValidationResult.incorrect(ErrorType.bit_groups_wrong_value(), %{
                 ParamKey.got() => "11111 000000"
               })
    end

    test "count checked before per-group checks", %{step: step} do
      result = AnswerValidator.validate(step, {:bit_groups, ["ABCDE"]})
      assert result.error_type == ErrorType.bit_groups_wrong_group_count()
    end

    test "alphabet checked before length on same group", %{step: step} do
      result = AnswerValidator.validate(step, {:bit_groups, ["00011", "AB"]})
      assert result.error_type == ErrorType.bit_groups_invalid_character()
    end
  end

  describe "hex-bytes" do
    setup do
      %{step: Step.HexBytes.new!([0xC3, 0xA9])}
    end

    test "correct value -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [0xC3, 0xA9]}) ==
               ValidationResult.correct()
    end

    test "empty -> hex-bytes.empty", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, []}) ==
               ValidationResult.incorrect(ErrorType.hex_bytes_empty())
    end

    test "byte > 255 -> hex-bytes.byte-out-of-range", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [0xC3, 0x100]}) ==
               ValidationResult.incorrect(ErrorType.hex_bytes_byte_out_of_range(), %{
                 ParamKey.position() => "1",
                 ParamKey.got() => "256"
               })
    end

    test "byte < 0 -> hex-bytes.byte-out-of-range", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [-1, 0xA9]}) ==
               ValidationResult.incorrect(ErrorType.hex_bytes_byte_out_of_range(), %{
                 ParamKey.position() => "0",
                 ParamKey.got() => "-1"
               })
    end

    test "too few bytes -> hex-bytes.too-few-bytes", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [0xC3]}) ==
               ValidationResult.incorrect(ErrorType.hex_bytes_too_few_bytes(), %{
                 ParamKey.expected_count() => "2",
                 ParamKey.got_count() => "1"
               })
    end

    test "too many bytes -> hex-bytes.too-many-bytes", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [0xC3, 0xA9, 0x00]}) ==
               ValidationResult.incorrect(ErrorType.hex_bytes_too_many_bytes(), %{
                 ParamKey.expected_count() => "2",
                 ParamKey.got_count() => "3"
               })
    end

    test "right count but wrong bytes -> hex-bytes.wrong-value (no expected leak)", %{step: step} do
      assert AnswerValidator.validate(step, {:hex_bytes, [0xC3, 0xAA]}) ==
               ValidationResult.incorrect(ErrorType.hex_bytes_wrong_value(), %{
                 ParamKey.got() => "C3 AA"
               })
    end

    test "range checked before count", %{step: step} do
      result = AnswerValidator.validate(step, {:hex_bytes, [0xC3, 0xA9, 300]})
      assert result.error_type == ErrorType.hex_bytes_byte_out_of_range()
    end
  end

  describe "code-point" do
    setup do
      %{step: Step.CodePointEntry.new!(0x00E9)}
    end

    test "correct value -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:code_point, 0x00E9}) == ValidationResult.correct()
    end

    test "negative -> code-point.out-of-range", %{step: step} do
      assert AnswerValidator.validate(step, {:code_point, -1}) ==
               ValidationResult.incorrect(ErrorType.code_point_out_of_range(), %{
                 ParamKey.got() => "-1",
                 ParamKey.min() => "0",
                 ParamKey.max() => "0x10FFFF"
               })
    end

    test "above U+10FFFF -> code-point.out-of-range", %{step: step} do
      assert AnswerValidator.validate(step, {:code_point, 0x110000}) ==
               ValidationResult.incorrect(ErrorType.code_point_out_of_range(), %{
                 ParamKey.got() => "1114112",
                 ParamKey.min() => "0",
                 ParamKey.max() => "0x10FFFF"
               })
    end

    test "high surrogate -> code-point.surrogate", %{step: step} do
      assert AnswerValidator.validate(step, {:code_point, 0xD800}) ==
               ValidationResult.incorrect(ErrorType.code_point_surrogate(), %{
                 ParamKey.got() => "U+D800"
               })
    end

    test "low surrogate -> code-point.surrogate", %{step: step} do
      assert AnswerValidator.validate(step, {:code_point, 0xDFFF}) ==
               ValidationResult.incorrect(ErrorType.code_point_surrogate(), %{
                 ParamKey.got() => "U+DFFF"
               })
    end

    test "valid but wrong -> code-point.wrong-value (no expected leak)", %{step: step} do
      assert AnswerValidator.validate(step, {:code_point, 0x00EA}) ==
               ValidationResult.incorrect(ErrorType.code_point_wrong_value(), %{
                 ParamKey.got() => "U+00EA"
               })
    end

    test "range checked before surrogate", %{step: step} do
      result = AnswerValidator.validate(step, {:code_point, 0x110000})
      assert result.error_type == ErrorType.code_point_out_of_range()
    end
  end

  describe "endianness" do
    setup do
      %{step: Step.Endianness.new!(:big)}
    end

    test "correct big endian -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:endianness, :big}) == ValidationResult.correct()
    end

    test "correct little endian (LE step) -> ok" do
      step = Step.Endianness.new!(:little)
      assert AnswerValidator.validate(step, {:endianness, :little}) == ValidationResult.correct()
    end

    test "wrong choice -> endianness.wrong-choice (no got/expected leak)", %{step: step} do
      assert AnswerValidator.validate(step, {:endianness, :little}) ==
               ValidationResult.incorrect(ErrorType.endianness_wrong_choice())
    end

    test "wrong answer type -> answer.type-mismatch", %{step: step} do
      result = AnswerValidator.validate(step, {:binary, "0"})
      assert result.error_type == ErrorType.answer_type_mismatch()
      assert result.params[ParamKey.expected_type()] == "endianness"
    end
  end

  describe "useful-bit-count" do
    setup do
      %{step: Step.UsefulBitCount.new!(11)}
    end

    test "correct value -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:useful_bit_count, 11}) ==
               ValidationResult.correct()
    end

    test "zero -> useful-bit-count.non-positive", %{step: step} do
      assert AnswerValidator.validate(step, {:useful_bit_count, 0}) ==
               ValidationResult.incorrect(ErrorType.useful_bit_count_non_positive(), %{
                 ParamKey.got() => "0"
               })
    end

    test "negative -> useful-bit-count.non-positive", %{step: step} do
      assert AnswerValidator.validate(step, {:useful_bit_count, -3}) ==
               ValidationResult.incorrect(ErrorType.useful_bit_count_non_positive(), %{
                 ParamKey.got() => "-3"
               })
    end

    test "wrong positive value -> useful-bit-count.wrong-value", %{step: step} do
      assert AnswerValidator.validate(step, {:useful_bit_count, 16}) ==
               ValidationResult.incorrect(ErrorType.useful_bit_count_wrong_value(), %{
                 ParamKey.got() => "16"
               })
    end

    test "wrong answer type -> answer.type-mismatch", %{step: step} do
      result = AnswerValidator.validate(step, {:binary, "11"})
      assert result.error_type == ErrorType.answer_type_mismatch()
      assert result.params[ParamKey.expected_type()] == "useful-bit-count"
    end
  end

  describe "offset" do
    setup do
      %{step: Step.Offset.new!(0xF389)}
    end

    test "correct value -> ok", %{step: step} do
      assert AnswerValidator.validate(step, {:offset, 0xF389}) == ValidationResult.correct()
    end

    test "negative -> offset.out-of-range", %{step: step} do
      assert AnswerValidator.validate(step, {:offset, -1}) ==
               ValidationResult.incorrect(ErrorType.offset_out_of_range(), %{
                 ParamKey.got() => "-1",
                 ParamKey.min() => "0",
                 ParamKey.max() => "0xFFFFF"
               })
    end

    test "above 0xFFFFF -> offset.out-of-range", %{step: step} do
      assert AnswerValidator.validate(step, {:offset, 0x100000}) ==
               ValidationResult.incorrect(ErrorType.offset_out_of_range(), %{
                 ParamKey.got() => "1048576",
                 ParamKey.min() => "0",
                 ParamKey.max() => "0xFFFFF"
               })
    end

    test "valid but wrong -> offset.wrong-value (no expected leak)", %{step: step} do
      assert AnswerValidator.validate(step, {:offset, 0xF38A}) ==
               ValidationResult.incorrect(ErrorType.offset_wrong_value(), %{
                 ParamKey.got() => "0xF38A"
               })
    end

    test "wrong answer type -> answer.type-mismatch", %{step: step} do
      result = AnswerValidator.validate(step, {:binary, "11"})
      assert result.error_type == ErrorType.answer_type_mismatch()
      assert result.params[ParamKey.expected_type()] == "offset"
    end
  end
end
