defmodule Charset.Exercise.ErrorType do
  @moduledoc """
  Stable identifiers for validation errors produced by `AnswerValidator`.

  These are business-event identifiers that the frontend consumes as i18n
  keys (`feedback.{error_type}.level{hint_level}`), NOT translations. Never
  use a string literal at a call site - always go through these functions, so
  a rename propagates everywhere (validator, tests, UI mapping).
  """

  def answer_type_mismatch, do: "answer.type-mismatch"

  def binary_empty, do: "binary.empty"
  def binary_invalid_character, do: "binary.invalid-character"
  def binary_too_few_bits, do: "binary.too-few-bits"
  def binary_too_many_bits, do: "binary.too-many-bits"
  def binary_wrong_value, do: "binary.wrong-value"

  def format_empty, do: "format.empty"
  def format_unknown_choice, do: "format.unknown-choice"
  def format_wrong_choice, do: "format.wrong-choice"

  def bit_groups_empty, do: "bit-groups.empty"
  def bit_groups_wrong_group_count, do: "bit-groups.wrong-group-count"
  def bit_groups_invalid_character, do: "bit-groups.invalid-character"
  def bit_groups_wrong_group_length, do: "bit-groups.wrong-group-length"
  def bit_groups_wrong_value, do: "bit-groups.wrong-value"

  def hex_bytes_empty, do: "hex-bytes.empty"
  def hex_bytes_byte_out_of_range, do: "hex-bytes.byte-out-of-range"
  def hex_bytes_too_few_bytes, do: "hex-bytes.too-few-bytes"
  def hex_bytes_too_many_bytes, do: "hex-bytes.too-many-bytes"
  def hex_bytes_wrong_value, do: "hex-bytes.wrong-value"

  def code_point_out_of_range, do: "code-point.out-of-range"
  def code_point_surrogate, do: "code-point.surrogate"
  def code_point_wrong_value, do: "code-point.wrong-value"

  def endianness_wrong_choice, do: "endianness.wrong-choice"

  def useful_bit_count_non_positive, do: "useful-bit-count.non-positive"
  def useful_bit_count_wrong_value, do: "useful-bit-count.wrong-value"

  def offset_out_of_range, do: "offset.out-of-range"
  def offset_wrong_value, do: "offset.wrong-value"
end
