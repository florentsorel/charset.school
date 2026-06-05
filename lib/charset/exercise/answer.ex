defmodule Charset.Exercise.Answer do
  @moduledoc """
  A user answer for one step, as a tagged tuple - light to build from the
  LiveView form layer and pattern-matchable in the validator:

      {:format, "format-choice.byte-count.2"}
      {:binary, "11000011"}
      {:bit_groups, ["00011", "101001"]}
      {:hex_bytes, [0xC3, 0xA9]}
      {:code_point, 0xE9}
      {:useful_bit_count, 11}
      {:endianness, :big}
      {:offset, 0x0F389}

  `type_id/1` exposes the same stable ids as the steps, used by the
  `answer.type-mismatch` params (an improvement over the old Kotlin
  class-name-based `got-type`).
  """

  @type t ::
          {:format, String.t()}
          | {:binary, String.t()}
          | {:bit_groups, [String.t()]}
          | {:hex_bytes, [integer()]}
          | {:code_point, integer()}
          | {:useful_bit_count, integer()}
          | {:endianness, Charset.Encoding.endian()}
          | {:offset, integer()}

  @spec type_id(t()) :: String.t()
  def type_id({:format, _value}), do: "format"
  def type_id({:binary, _bits}), do: "binary"
  def type_id({:bit_groups, _groups}), do: "bit-groups"
  def type_id({:hex_bytes, _bytes}), do: "hex-bytes"
  def type_id({:code_point, _value}), do: "code-point"
  def type_id({:useful_bit_count, _value}), do: "useful-bit-count"
  def type_id({:endianness, _value}), do: "endianness"
  def type_id({:offset, _value}), do: "offset"
end
