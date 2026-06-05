defmodule Charset.Exercise.Step.UsefulBitCount do
  @moduledoc """
  Useful-bit-count step (UTF-8 exercise flow): after padding to a byte
  multiple, the user states how many bits are actually useful.
  """

  @enforce_keys [:expected]
  defstruct [:expected]

  @type t :: %__MODULE__{expected: 1..32}

  @spec new!(integer()) :: t()
  def new!(expected) do
    unless is_integer(expected) and expected in 1..32 do
      raise ArgumentError, "UsefulBitCount expected must be in 1..32, got #{inspect(expected)}"
    end

    %__MODULE__{expected: expected}
  end
end
