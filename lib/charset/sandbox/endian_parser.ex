defmodule Charset.Sandbox.EndianParser do
  @moduledoc """
  Parses the endianness selector value of the UTF-16/32 sandbox pages.
  """

  @type reason :: :invalid

  @spec parse(String.t()) :: {:ok, Charset.Encoding.endian()} | {:error, reason()}
  def parse(raw) when is_binary(raw) do
    case raw |> String.trim() |> String.downcase() do
      "big" -> {:ok, :big}
      "little" -> {:ok, :little}
      _other -> {:error, :invalid}
    end
  end
end
