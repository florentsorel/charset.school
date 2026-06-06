defmodule Charset.Exercise.Step do
  @moduledoc """
  Helpers over the step structs (`Charset.Exercise.Step.*`).

  A step is one micro-question inside an exercise. Each struct carries the
  server-side `expected` answer - it never leaves the server (anti-cheat);
  the UI only receives the structural fields it needs to render the widget.

  The string ids are the stable identifiers shared with the DB
  (`attempt_steps.step_type`) and the frontend.
  """

  alias Charset.Exercise.Step

  @type t ::
          Step.Format.t()
          | Step.Binary.t()
          | Step.BitGroups.t()
          | Step.HexBytes.t()
          | Step.CodePointEntry.t()
          | Step.UsefulBitCount.t()
          | Step.Endianness.t()
          | Step.Offset.t()

  @doc """
  The stable string id of a step's type.

      iex> Charset.Exercise.Step.type_id(%Charset.Exercise.Step.CodePointEntry{expected: 0xE9})
      "code-point"
  """
  @spec type_id(t()) :: String.t()
  def type_id(%Step.Format{}), do: "format"
  def type_id(%Step.Binary{}), do: "binary"
  def type_id(%Step.BitGroups{}), do: "bit-groups"
  def type_id(%Step.HexBytes{}), do: "hex-bytes"
  def type_id(%Step.CodePointEntry{}), do: "code-point"
  def type_id(%Step.UsefulBitCount{}), do: "useful-bit-count"
  def type_id(%Step.Endianness{}), do: "endianness"
  def type_id(%Step.Offset{}), do: "offset"
end
