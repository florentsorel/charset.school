defmodule Charset.Sandbox.InputParserTest do
  use ExUnit.Case, async: true

  alias Charset.Sandbox.InputParser

  test "parses U+XXXX form" do
    assert InputParser.parse("U+00E9") == {:ok, 0xE9}
    assert InputParser.parse("u+1f389") == {:ok, 0x1F389}
  end

  test "parses 0xXX form" do
    assert InputParser.parse("0xE9") == {:ok, 0xE9}
    assert InputParser.parse("0X1F389") == {:ok, 0x1F389}
  end

  test "parses decimal" do
    assert InputParser.parse("233") == {:ok, 0xE9}
    assert InputParser.parse("65") == {:ok, 0x41}
  end

  test "parses a single character via its code point" do
    assert InputParser.parse("é") == {:ok, 0xE9}
    assert InputParser.parse("A") == {:ok, 0x41}
    assert InputParser.parse("🎉") == {:ok, 0x1F389}
  end

  test "trims whitespace" do
    assert InputParser.parse("  U+00E9  ") == {:ok, 0xE9}
  end

  test "rejects empty input" do
    assert InputParser.parse("") == {:error, :empty}
    assert InputParser.parse("   ") == {:error, :empty}
  end

  test "rejects unparseable input" do
    assert InputParser.parse("abc") == {:error, :unparseable}
    assert InputParser.parse("U+ZZZZ") == {:error, :unparseable}
    assert InputParser.parse("AB") == {:error, :unparseable}
  end

  test "rejects out-of-range code points" do
    assert InputParser.parse("U+110000") == {:error, :out_of_range}
    assert InputParser.parse("1114112") == {:error, :out_of_range}
  end

  test "rejects huge decimal inputs as out_of_range" do
    assert InputParser.parse("9999999999") == {:error, :out_of_range}
    assert InputParser.parse("99999999999999999999") == {:error, :out_of_range}
  end

  test "rejects surrogate code points" do
    assert InputParser.parse("U+D800") == {:error, :surrogate}
    assert InputParser.parse("U+DFFF") == {:error, :surrogate}
    assert InputParser.parse("55296") == {:error, :surrogate}
  end
end
