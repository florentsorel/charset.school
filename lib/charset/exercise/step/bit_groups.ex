defmodule Charset.Exercise.Step.BitGroups do
  @moduledoc """
  Split-the-bits step: the user splits the useful bits into per-byte (or
  per-code-unit) packets. The group lengths are public structure; the group
  values stay server-side.
  """

  alias Charset.Exercise.Step.Binary

  @enforce_keys [:expected]
  defstruct [:expected]

  @type t :: %__MODULE__{expected: [String.t()]}

  @spec new!([String.t()]) :: t()
  def new!(expected) do
    if expected == [] do
      raise ArgumentError, "BitGroups step must have at least one group"
    end

    expected
    |> Enum.with_index()
    |> Enum.each(fn {group, index} ->
      if group == "" do
        raise ArgumentError, "BitGroups group at position #{index} is empty"
      end

      unless Binary.bits?(group) do
        raise ArgumentError,
              "BitGroups group at position #{index} must contain only 0/1 characters"
      end
    end)

    %__MODULE__{expected: expected}
  end
end
