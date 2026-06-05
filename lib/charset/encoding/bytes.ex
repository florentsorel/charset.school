defmodule Charset.Encoding.Bytes do
  @moduledoc """
  Byte sequence formatting helpers.
  """

  @doc """
  Formats a binary as space-separated uppercase hex pairs.

      iex> Charset.Encoding.Bytes.to_hex(<<0xC3, 0xA9>>)
      "C3 A9"

      iex> Charset.Encoding.Bytes.to_hex(<<>>)
      ""
  """
  @spec to_hex(binary()) :: String.t()
  def to_hex(bytes) when is_binary(bytes) do
    bytes
    |> :binary.bin_to_list()
    |> Enum.map_join(" ", fn byte ->
      byte |> Integer.to_string(16) |> String.pad_leading(2, "0")
    end)
  end
end
