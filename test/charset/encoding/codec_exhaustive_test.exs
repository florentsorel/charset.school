defmodule Charset.Encoding.CodecExhaustiveTest do
  @moduledoc """
  Exhaustive sweeps over all 1,112,064 valid code points (0..0x10FFFF minus
  the surrogate range):

    * our encoder must agree byte-for-byte with the BEAM's native bitstring
      encoder (`<<cp::utf8>>`, `::utf16-big/little`, `::utf32-big/little`)
    * decode(encode(cp)) must roundtrip for every encoding

  The example-based cases (boundaries, error messages) live in `codec_test.exs`;
  these sweeps only cover the happy path, but cover ALL of it.
  """
  use ExUnit.Case, async: true

  alias Charset.Encoding.Codec
  alias Charset.Encoding.Windows1252

  # All valid (non-surrogate) Unicode code points.
  defp valid_code_points, do: Stream.concat(0x0000..0xD7FF, 0xE000..0x10FFFF)

  describe "native oracle - every valid code point encodes like the BEAM" do
    test "utf-8" do
      mismatches =
        valid_code_points()
        |> Enum.reject(fn cp -> Codec.encode!(cp, :utf8) == <<cp::utf8>> end)

      assert mismatches == []
    end

    test "utf-16 be" do
      mismatches =
        valid_code_points()
        |> Enum.reject(fn cp -> Codec.encode!(cp, :utf16be) == <<cp::utf16-big>> end)

      assert mismatches == []
    end

    test "utf-16 le" do
      mismatches =
        valid_code_points()
        |> Enum.reject(fn cp -> Codec.encode!(cp, :utf16le) == <<cp::utf16-little>> end)

      assert mismatches == []
    end

    test "utf-32 be" do
      mismatches =
        valid_code_points()
        |> Enum.reject(fn cp -> Codec.encode!(cp, :utf32be) == <<cp::utf32-big>> end)

      assert mismatches == []
    end

    test "utf-32 le" do
      mismatches =
        valid_code_points()
        |> Enum.reject(fn cp -> Codec.encode!(cp, :utf32le) == <<cp::utf32-little>> end)

      assert mismatches == []
    end
  end

  describe "roundtrip - decode(encode(cp)) == cp for every encodable code point" do
    for encoding <- [:utf8, :utf16be, :utf16le, :utf32be, :utf32le] do
      test "#{encoding} over the full Unicode range" do
        encoding = unquote(encoding)

        broken =
          valid_code_points()
          |> Enum.reject(fn cp -> Codec.decode!(Codec.encode!(cp, encoding), encoding) == cp end)

        assert broken == []
      end
    end

    test "ascii over 0x00..0x7F" do
      for cp <- 0x00..0x7F do
        assert Codec.decode!(Codec.encode!(cp, :ascii), :ascii) == cp
      end
    end

    test "latin1 over 0x00..0xFF" do
      for cp <- 0x00..0xFF do
        assert Codec.decode!(Codec.encode!(cp, :latin1), :latin1) == cp
      end
    end

    test "windows-1252 over its 251 encodable code points" do
      for cp <- Windows1252.encodable_code_points() do
        assert Codec.decode!(Codec.encode!(cp, :windows1252), :windows1252) == cp
      end
    end
  end

  describe "surrogates are rejected everywhere" do
    test "encode errors for every surrogate in every UTF encoding" do
      for cp <- 0xD800..0xDFFF, encoding <- [:utf8, :utf16be, :utf16le, :utf32be, :utf32le] do
        assert {:error, %{reason: :surrogate}} = Codec.encode(cp, encoding)
      end
    end
  end
end
