defmodule Charset.Exercise.Step.Binary do
  @moduledoc """
  Free-input binary step: the user types the bit string. `length` is public
  structure (the UI renders that many bit boxes); `expected` stays server-side.
  """

  @enforce_keys [:expected, :length]
  defstruct [:expected, :length]

  @type t :: %__MODULE__{expected: String.t(), length: pos_integer()}

  @spec new!(String.t(), pos_integer()) :: t()
  def new!(expected, length) do
    unless is_integer(length) and length > 0 do
      raise ArgumentError, "Binary step length must be positive, got #{inspect(length)}"
    end

    unless String.length(expected) == length do
      raise ArgumentError,
            "Binary step expected length must equal length: " <>
              "expected.length=#{String.length(expected)}, length=#{length}"
    end

    unless bits?(expected) do
      raise ArgumentError, "Binary step expected must contain only 0/1 characters"
    end

    %__MODULE__{expected: expected, length: length}
  end

  @doc "Whether the string contains only 0/1 characters."
  @spec bits?(String.t()) :: boolean()
  def bits?(string), do: string =~ ~r/^[01]*$/
end
