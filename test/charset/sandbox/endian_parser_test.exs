defmodule Charset.Sandbox.EndianParserTest do
  use ExUnit.Case, async: true

  alias Charset.Sandbox.EndianParser

  test "parses 'big' as :big" do
    assert EndianParser.parse("big") == {:ok, :big}
    assert EndianParser.parse("BIG") == {:ok, :big}
    assert EndianParser.parse("  Big  ") == {:ok, :big}
  end

  test "parses 'little' as :little" do
    assert EndianParser.parse("little") == {:ok, :little}
    assert EndianParser.parse("LITTLE") == {:ok, :little}
    assert EndianParser.parse("Little") == {:ok, :little}
  end

  test "rejects unknown values" do
    assert EndianParser.parse("be") == {:error, :invalid}
    assert EndianParser.parse("le") == {:error, :invalid}
    assert EndianParser.parse("") == {:error, :invalid}
    assert EndianParser.parse("garbage") == {:error, :invalid}
  end
end
