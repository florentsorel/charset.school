defmodule Charset.Exercise.Step.Offset do
  @moduledoc """
  Surrogate-pair offset step (UTF-16 supplementary flows): the 20-bit value
  obtained by subtracting 0x10000 from the code point, entered in hex.
  """

  @enforce_keys [:expected]
  defstruct [:expected]

  @type t :: %__MODULE__{expected: 0..0xFFFFF}

  @spec new!(integer()) :: t()
  def new!(expected) do
    unless is_integer(expected) and expected in 0..0xFFFFF do
      raise ArgumentError,
            "Offset step expected must be a 20-bit value (0..0xFFFFF), got #{inspect(expected)}"
    end

    %__MODULE__{expected: expected}
  end
end
