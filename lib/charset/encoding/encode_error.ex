defmodule Charset.Encoding.EncodeError do
  @moduledoc """
  Raised (or returned, in the non-bang API) when a code point cannot be
  represented in the target encoding.

  `reason` is a stable atom meant to be pattern-matched by callers (the
  sandbox maps reasons to i18n keys); the `message/1` string is dev-facing
  English for logs, mirroring the old Kotlin `EncoderException` wording.

  Reasons:

    * `:out_of_range` - the code point exceeds the encoding's range (ASCII,
      Latin-1)
    * `:not_representable` - the code point has no Windows-1252 byte
    * `:surrogate` - surrogate code points are not encodable (UTF-8/16/32)
  """

  alias Charset.Encoding
  alias Charset.Encoding.CodePoint

  @type reason :: :out_of_range | :not_representable | :surrogate

  @type t :: %__MODULE__{
          code_point: integer(),
          encoding: Encoding.t(),
          reason: reason()
        }

  defexception [:code_point, :encoding, :reason]

  @impl Exception
  def message(%__MODULE__{code_point: code_point, encoding: encoding, reason: reason}) do
    "Cannot encode #{CodePoint.format(code_point)} in #{Encoding.id(encoding)}: " <>
      describe(reason, encoding)
  end

  defp describe(:out_of_range, :ascii), do: "value exceeds U+007F"
  defp describe(:out_of_range, :latin1), do: "value exceeds U+00FF"
  defp describe(:not_representable, :windows1252), do: "not representable in Windows-1252"
  defp describe(:surrogate, :utf8), do: "surrogate not encodable in UTF-8"

  defp describe(:surrogate, encoding) when encoding in [:utf16be, :utf16le],
    do: "surrogate not encodable standalone"

  defp describe(:surrogate, encoding) when encoding in [:utf32be, :utf32le],
    do: "surrogate not encodable in UTF-32"
end
