defmodule Charset.Exercise.Step.HexBytes do
  @moduledoc """
  Hex bytes step: the user types each byte in hex. The byte count is public
  structure (N hex boxes); the values stay server-side.
  """

  @enforce_keys [:expected]
  defstruct [:expected]

  @type t :: %__MODULE__{expected: [0..255]}

  @spec new!([integer()]) :: t()
  def new!(expected) do
    if expected == [] do
      raise ArgumentError, "HexBytes step must have at least one byte"
    end

    expected
    |> Enum.with_index()
    |> Enum.each(fn {byte, index} ->
      unless is_integer(byte) and byte in 0..0xFF do
        raise ArgumentError,
              "HexBytes step byte at position #{index} must be in 0..255, got #{inspect(byte)}"
      end
    end)

    %__MODULE__{expected: expected}
  end
end
