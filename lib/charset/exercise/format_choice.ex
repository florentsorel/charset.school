defmodule Charset.Exercise.FormatChoice do
  @moduledoc """
  Stable identifiers for the options displayed by a `Step.Format` choice.

  Same convention as `ErrorType` and `ParamKey`: domain-side identifiers that
  the frontend consumes as i18n keys (e.g. `format-choice.byte-count.2` →
  "2 octets" in FR, "2 bytes" in EN).

  The user clicks a button labelled with the translation. The frontend sends
  back the **identifier** (not the translation), which `AnswerValidator`
  compares to `Step.Format.expected` - the validator is locale-agnostic.
  """

  # Byte count choices used by UTF-8 (1-4 bytes), UTF-16 (2 or 4 bytes),
  # and UTF-32 (always 4 bytes - others are pedagogical "decoys").
  def one_byte, do: "format-choice.byte-count.1"
  def two_bytes, do: "format-choice.byte-count.2"
  def three_bytes, do: "format-choice.byte-count.3"
  def four_bytes, do: "format-choice.byte-count.4"

  @doc "Indexed by byte count: byte_count_choices() |> Enum.at(count - 1)."
  def byte_count_choices, do: [one_byte(), two_bytes(), three_bytes(), four_bytes()]

  # Code-unit count choices used by UTF-16 (1 or 2 code units). A UTF-16
  # code unit is 16 bits (2 bytes); surrogate pairs span 2 units = 4 bytes.
  def one_code_unit, do: "format-choice.code-unit.1"
  def two_code_units, do: "format-choice.code-unit.2"

  @doc "Indexed by code-unit count: code_unit_choices() |> Enum.at(count - 1)."
  def code_unit_choices, do: [one_code_unit(), two_code_units()]
end
