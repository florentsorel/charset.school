defmodule Charset.Sandbox.BytesParser do
  @moduledoc """
  Parses the free-form text the sandbox user types to enter a byte sequence
  (decode flows). Accepted shapes are intentionally generous, any of:

      C3 A9          (hex pairs separated by spaces)
      C3,A9          (comma-separated)
      C3-A9          (dash-separated)
      C3A9           (no separator)
      0xC3 0xA9      (with hex prefix per byte)
      0xC3,0xA9      (combinations)

  Returns `{:error, reason}` with a stable reason atom (`:empty`,
  `:invalid_hex`, `:odd_length`, `:too_long`) that the UI maps to its i18n
  keys. Mirrors `Charset.Sandbox.InputParser` for the encode flow.
  """

  @max_bytes 4

  @type reason :: :empty | :invalid_hex | :odd_length | :too_long

  @spec max_bytes() :: pos_integer()
  def max_bytes, do: @max_bytes

  @spec parse(String.t()) :: {:ok, binary()} | {:error, reason()}
  def parse(raw) when is_binary(raw) do
    # Strip every 0x/0X prefix (with or without surrounding separators) and
    # every separator, leaving only hex digits.
    normalized =
      raw
      |> String.trim()
      |> String.replace(~r/0[xX]/, "")
      |> String.replace(~r/[\s,;-]+/, "")

    cond do
      String.trim(raw) == "" -> {:error, :empty}
      normalized == "" -> {:error, :empty}
      not (normalized =~ ~r/^[0-9a-fA-F]+$/) -> {:error, :invalid_hex}
      rem(String.length(normalized), 2) != 0 -> {:error, :odd_length}
      div(String.length(normalized), 2) > @max_bytes -> {:error, :too_long}
      true -> {:ok, Base.decode16!(normalized, case: :mixed)}
    end
  end
end
