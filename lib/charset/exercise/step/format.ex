defmodule Charset.Exercise.Step.Format do
  @moduledoc """
  Multiple-choice step: the user picks the format (byte count, code-unit
  count). `choices` are `Charset.Exercise.FormatChoice` identifiers, already
  public in the UI.
  """

  @enforce_keys [:choices, :expected]
  defstruct [:choices, :expected]

  @type t :: %__MODULE__{choices: [String.t()], expected: String.t()}

  @spec new!([String.t()], String.t()) :: t()
  def new!(choices, expected) do
    if choices == [] do
      raise ArgumentError, "Format step must offer at least one choice"
    end

    unless expected in choices do
      raise ArgumentError, "Format step expected '#{expected}' must be one of the offered choices"
    end

    %__MODULE__{choices: choices, expected: expected}
  end
end
