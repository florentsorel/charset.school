defmodule Charset.Exercise.ParamKey do
  @moduledoc """
  Names of the interpolation variables exposed in `ValidationResult.params`.

  Same convention as `ErrorType`: stable identifiers, never written as string
  literals at call sites.
  """

  def got, do: "got"
  def got_type, do: "got-type"
  def expected_type, do: "expected-type"
  def expected_length, do: "expected-length"
  def got_length, do: "got-length"
  def expected_count, do: "expected-count"
  def got_count, do: "got-count"
  def position, do: "position"
  def choices, do: "choices"
  def min, do: "min"
  def max, do: "max"
end
