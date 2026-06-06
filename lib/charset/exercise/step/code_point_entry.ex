defmodule Charset.Exercise.Step.CodePointEntry do
  @moduledoc """
  Code point entry step (decode flows): the user types the decoded code point.
  """

  import Charset.Encoding.CodePoint, only: [is_code_point: 1, is_surrogate: 1]

  alias Charset.Encoding.CodePoint

  @enforce_keys [:expected]
  defstruct [:expected]

  @type t :: %__MODULE__{expected: 0..0x10FFFF}

  @spec new!(integer()) :: t()
  def new!(expected) do
    unless is_code_point(expected) do
      raise ArgumentError,
            "CodePoint step expected must be in Unicode range, got #{inspect(expected)}"
    end

    if is_surrogate(expected) do
      raise ArgumentError,
            "CodePoint step expected must not be a surrogate, got #{CodePoint.format(expected)}"
    end

    %__MODULE__{expected: expected}
  end
end
