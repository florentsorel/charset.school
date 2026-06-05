defmodule Charset.Exercise.AnswerValidator do
  @moduledoc """
  Validates one `(step, answer)` couple at a time and reports a
  `ValidationResult` with a stable `error_type` + interpolation `params`.

  The validator is exercise-agnostic (no encoding, code point or level
  knowledge): composition of steps is the generators' job.

  Anti-cheat rules (see AGENTS.md): `params` may echo the user's own input
  (`got`) and public structure (lengths, counts, positions, public bounds,
  the already-displayed choices) - never the expected value. Special case:
  `endianness` has only two possible values, so even `got` would reveal the
  answer by deduction - the error type alone is the feedback.
  """

  import Charset.Encoding.CodePoint, only: [is_code_point: 1, is_surrogate: 1]

  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Answer
  alias Charset.Exercise.ErrorType
  alias Charset.Exercise.ParamKey
  alias Charset.Exercise.Step
  alias Charset.Exercise.ValidationResult

  @spec validate(Step.t(), Answer.t()) :: ValidationResult.t()

  ## Format

  def validate(%Step.Format{} = step, {:format, value}) do
    cond do
      value == "" ->
        ValidationResult.incorrect(ErrorType.format_empty())

      # The choices list is public (already shown in the UI) - safe to echo back.
      value not in step.choices ->
        ValidationResult.incorrect(ErrorType.format_unknown_choice(), %{
          ParamKey.got() => value,
          ParamKey.choices() => Enum.join(step.choices, ", ")
        })

      value != step.expected ->
        ValidationResult.incorrect(ErrorType.format_wrong_choice(), %{ParamKey.got() => value})

      true ->
        ValidationResult.correct()
    end
  end

  ## Binary

  def validate(%Step.Binary{} = step, {:binary, bits}) do
    # Pedagogical order: alphabet first, then length, then value.
    cond do
      bits == "" ->
        ValidationResult.incorrect(ErrorType.binary_empty())

      not Step.Binary.bits?(bits) ->
        ValidationResult.incorrect(ErrorType.binary_invalid_character(), %{
          ParamKey.got() => bits
        })

      String.length(bits) < step.length ->
        ValidationResult.incorrect(ErrorType.binary_too_few_bits(), %{
          ParamKey.expected_length() => Integer.to_string(step.length),
          ParamKey.got_length() => Integer.to_string(String.length(bits))
        })

      String.length(bits) > step.length ->
        ValidationResult.incorrect(ErrorType.binary_too_many_bits(), %{
          ParamKey.expected_length() => Integer.to_string(step.length),
          ParamKey.got_length() => Integer.to_string(String.length(bits))
        })

      bits != step.expected ->
        ValidationResult.incorrect(ErrorType.binary_wrong_value(), %{ParamKey.got() => bits})

      true ->
        ValidationResult.correct()
    end
  end

  ## BitGroups

  def validate(%Step.BitGroups{} = step, {:bit_groups, groups}) do
    cond do
      groups == [] ->
        ValidationResult.incorrect(ErrorType.bit_groups_empty())

      length(groups) != length(step.expected) ->
        ValidationResult.incorrect(ErrorType.bit_groups_wrong_group_count(), %{
          ParamKey.expected_count() => Integer.to_string(length(step.expected)),
          ParamKey.got_count() => Integer.to_string(length(groups))
        })

      true ->
        validate_bit_group_contents(step, groups)
    end
  end

  ## HexBytes

  def validate(%Step.HexBytes{} = step, {:hex_bytes, bytes}) do
    invalid_index = Enum.find_index(bytes, fn byte -> byte not in 0..0xFF end)

    cond do
      bytes == [] ->
        ValidationResult.incorrect(ErrorType.hex_bytes_empty())

      # Any byte must fit in an octet: 0..255 unsigned.
      invalid_index != nil ->
        ValidationResult.incorrect(ErrorType.hex_bytes_byte_out_of_range(), %{
          ParamKey.position() => Integer.to_string(invalid_index),
          ParamKey.got() => Integer.to_string(Enum.at(bytes, invalid_index))
        })

      length(bytes) < length(step.expected) ->
        ValidationResult.incorrect(ErrorType.hex_bytes_too_few_bytes(), %{
          ParamKey.expected_count() => Integer.to_string(length(step.expected)),
          ParamKey.got_count() => Integer.to_string(length(bytes))
        })

      length(bytes) > length(step.expected) ->
        ValidationResult.incorrect(ErrorType.hex_bytes_too_many_bytes(), %{
          ParamKey.expected_count() => Integer.to_string(length(step.expected)),
          ParamKey.got_count() => Integer.to_string(length(bytes))
        })

      bytes != step.expected ->
        ValidationResult.incorrect(ErrorType.hex_bytes_wrong_value(), %{
          ParamKey.got() => Enum.map_join(bytes, " ", &hex_byte/1)
        })

      true ->
        ValidationResult.correct()
    end
  end

  ## CodePointEntry

  def validate(%Step.CodePointEntry{} = step, {:code_point, value}) do
    cond do
      # Unicode range is public info, not the answer - safe to echo bounds.
      not is_code_point(value) ->
        ValidationResult.incorrect(ErrorType.code_point_out_of_range(), %{
          ParamKey.got() => Integer.to_string(value),
          ParamKey.min() => "0",
          ParamKey.max() => "0x10FFFF"
        })

      is_surrogate(value) ->
        ValidationResult.incorrect(ErrorType.code_point_surrogate(), %{
          ParamKey.got() => CodePoint.format(value)
        })

      value != step.expected ->
        ValidationResult.incorrect(ErrorType.code_point_wrong_value(), %{
          ParamKey.got() => CodePoint.format(value)
        })

      true ->
        ValidationResult.correct()
    end
  end

  ## UsefulBitCount

  def validate(%Step.UsefulBitCount{} = step, {:useful_bit_count, value}) do
    cond do
      value <= 0 ->
        ValidationResult.incorrect(ErrorType.useful_bit_count_non_positive(), %{
          ParamKey.got() => Integer.to_string(value)
        })

      value != step.expected ->
        ValidationResult.incorrect(ErrorType.useful_bit_count_wrong_value(), %{
          ParamKey.got() => Integer.to_string(value)
        })

      true ->
        ValidationResult.correct()
    end
  end

  ## Endianness

  def validate(%Step.Endianness{} = step, {:endianness, value}) do
    # With only two possible values, revealing "got" implicitly reveals the
    # answer. The error type alone is the feedback - no params.
    if value == step.expected do
      ValidationResult.correct()
    else
      ValidationResult.incorrect(ErrorType.endianness_wrong_choice())
    end
  end

  ## Offset

  def validate(%Step.Offset{} = step, {:offset, value}) do
    cond do
      # The 20-bit range is public structure, not the answer - safe to echo bounds.
      value not in 0..0xFFFFF ->
        ValidationResult.incorrect(ErrorType.offset_out_of_range(), %{
          ParamKey.got() => Integer.to_string(value),
          ParamKey.min() => "0",
          ParamKey.max() => "0xFFFFF"
        })

      value != step.expected ->
        ValidationResult.incorrect(ErrorType.offset_wrong_value(), %{
          ParamKey.got() => "0x" <> Integer.to_string(value, 16)
        })

      true ->
        ValidationResult.correct()
    end
  end

  ## Type mismatch (any step paired with an answer of another kind)

  def validate(step, answer) do
    ValidationResult.incorrect(ErrorType.answer_type_mismatch(), %{
      ParamKey.expected_type() => Step.type_id(step),
      ParamKey.got_type() => Answer.type_id(answer)
    })
  end

  ## Helpers

  # Per-group: alphabet first, then length (structural hints - no expected
  # value leak), then the whole list compared at once.
  defp validate_bit_group_contents(step, groups) do
    error =
      groups
      |> Enum.zip(step.expected)
      |> Enum.with_index()
      |> Enum.find_value(fn {{group, expected_group}, index} ->
        cond do
          not Step.Binary.bits?(group) ->
            ValidationResult.incorrect(ErrorType.bit_groups_invalid_character(), %{
              ParamKey.position() => Integer.to_string(index),
              ParamKey.got() => group
            })

          String.length(group) != String.length(expected_group) ->
            ValidationResult.incorrect(ErrorType.bit_groups_wrong_group_length(), %{
              ParamKey.position() => Integer.to_string(index),
              ParamKey.expected_length() => Integer.to_string(String.length(expected_group)),
              ParamKey.got_length() => Integer.to_string(String.length(group))
            })

          true ->
            nil
        end
      end)

    cond do
      error != nil ->
        error

      groups != step.expected ->
        ValidationResult.incorrect(ErrorType.bit_groups_wrong_value(), %{
          ParamKey.got() => Enum.join(groups, " ")
        })

      true ->
        ValidationResult.correct()
    end
  end

  defp hex_byte(byte), do: String.pad_leading(Integer.to_string(byte, 16), 2, "0")
end
