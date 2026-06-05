defmodule Charset.Exercise.Step.Endianness do
  @moduledoc """
  Endianness choice step (sandbox UTF-16/32 flows): big or little endian.
  """

  @enforce_keys [:expected]
  defstruct [:expected]

  @type t :: %__MODULE__{expected: Charset.Encoding.endian()}

  @spec new!(Charset.Encoding.endian()) :: t()
  def new!(expected) when expected in [:big, :little] do
    %__MODULE__{expected: expected}
  end
end
