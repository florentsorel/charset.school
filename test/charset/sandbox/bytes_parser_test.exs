defmodule Charset.Sandbox.BytesParserTest do
  use ExUnit.Case, async: true

  alias Charset.Sandbox.BytesParser

  test "parses contiguous hex without separator" do
    assert BytesParser.parse("C3A9") == {:ok, <<0xC3, 0xA9>>}
    assert BytesParser.parse("F09F8E89") == {:ok, <<0xF0, 0x9F, 0x8E, 0x89>>}
  end

  test "parses hex pairs separated by spaces" do
    assert BytesParser.parse("C3 A9") == {:ok, <<0xC3, 0xA9>>}
    assert BytesParser.parse("F0 9F 8E 89") == {:ok, <<0xF0, 0x9F, 0x8E, 0x89>>}
  end

  test "parses hex pairs separated by commas, dashes, semicolons" do
    assert BytesParser.parse("C3,A9") == {:ok, <<0xC3, 0xA9>>}
    assert BytesParser.parse("C3-A9") == {:ok, <<0xC3, 0xA9>>}
    assert BytesParser.parse("C3;A9") == {:ok, <<0xC3, 0xA9>>}
  end

  test "parses hex pairs with 0x / 0X prefix" do
    assert BytesParser.parse("0xC3 0xA9") == {:ok, <<0xC3, 0xA9>>}
    assert BytesParser.parse("0XC3,0XA9") == {:ok, <<0xC3, 0xA9>>}
  end

  test "accepts lower-case hex" do
    assert BytesParser.parse("c3a9") == {:ok, <<0xC3, 0xA9>>}
  end

  test "trims surrounding whitespace" do
    assert BytesParser.parse("   C3 A9   ") == {:ok, <<0xC3, 0xA9>>}
  end

  test "rejects empty input" do
    assert BytesParser.parse("") == {:error, :empty}
    assert BytesParser.parse("   ") == {:error, :empty}
    assert BytesParser.parse("0x") == {:error, :empty}
  end

  test "rejects non-hex characters" do
    assert BytesParser.parse("hello") == {:error, :invalid_hex}
    assert BytesParser.parse("ZZ") == {:error, :invalid_hex}
    assert BytesParser.parse("C3 GG") == {:error, :invalid_hex}
  end

  test "rejects odd-length hex (incomplete last byte)" do
    assert BytesParser.parse("C") == {:error, :odd_length}
    assert BytesParser.parse("C3A") == {:error, :odd_length}
    assert BytesParser.parse("C3 A") == {:error, :odd_length}
  end

  test "accepts up to 4 bytes (UTF-8 max per code point)" do
    assert BytesParser.parse("F09F8E89") == {:ok, <<0xF0, 0x9F, 0x8E, 0x89>>}
  end

  test "rejects inputs larger than 4 bytes" do
    assert BytesParser.parse("AA BB CC DD EE") == {:error, :too_long}
    assert BytesParser.parse(String.duplicate("00", 100)) == {:error, :too_long}
  end
end
