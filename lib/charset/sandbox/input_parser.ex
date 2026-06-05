defmodule Charset.Sandbox.InputParser do
  @moduledoc """
  Parses the free-form text the sandbox user types to designate a code point
  (encode flows). Accepted shapes: `U+00E9`, `0xE9`, decimal (`233`), or a
  single character (`é`, `🎉`).

  Returns `{:error, reason}` with a stable reason atom (`:empty`,
  `:unparseable`, `:out_of_range`, `:surrogate`) that the UI maps to its
  i18n keys - a wrong input is an expected user action, not a bug.
  """

  import Charset.Encoding.CodePoint, only: [is_code_point: 1, is_surrogate: 1]

  @type reason :: :empty | :unparseable | :out_of_range | :surrogate

  @u_plus_pattern ~r/^[Uu]\+([0-9a-fA-F]{1,6})$/
  @hex_pattern ~r/^0[xX]([0-9a-fA-F]{1,6})$/
  @decimal_pattern ~r/^\d+$/

  @spec parse(String.t()) :: {:ok, 0..0x10FFFF} | {:error, reason()}
  def parse(raw) when is_binary(raw) do
    input = String.trim(raw)

    cond do
      input == "" -> {:error, :empty}
      hex = capture(@u_plus_pattern, input) -> validate_range(String.to_integer(hex, 16))
      hex = capture(@hex_pattern, input) -> validate_range(String.to_integer(hex, 16))
      input =~ @decimal_pattern -> validate_range(String.to_integer(input))
      true -> parse_single_character(input)
    end
  end

  defp capture(pattern, input) do
    case Regex.run(pattern, input, capture: :all_but_first) do
      [hex] -> hex
      nil -> nil
    end
  end

  defp parse_single_character(input) do
    case String.to_charlist(input) do
      [code_point] -> validate_range(code_point)
      _other -> {:error, :unparseable}
    end
  end

  defp validate_range(value) do
    cond do
      not is_code_point(value) -> {:error, :out_of_range}
      is_surrogate(value) -> {:error, :surrogate}
      true -> {:ok, value}
    end
  end
end
