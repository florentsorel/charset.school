defmodule Charset.EncodingTest do
  use ExUnit.Case, async: true

  alias Charset.Encoding

  doctest Charset.Encoding

  describe "from_id/1" do
    test "returns the correct encoding for valid ids" do
      assert Encoding.from_id("ascii") == :ascii
      assert Encoding.from_id("latin1") == :latin1
      assert Encoding.from_id("windows-1252") == :windows1252
      assert Encoding.from_id("utf-8") == :utf8
      assert Encoding.from_id("utf-16be") == :utf16be
      assert Encoding.from_id("utf-16le") == :utf16le
      assert Encoding.from_id("utf-32be") == :utf32be
      assert Encoding.from_id("utf-32le") == :utf32le
    end

    test "returns nil for invalid ids" do
      assert Encoding.from_id("invalid-encoding") == nil
      assert Encoding.from_id("") == nil
    end

    test "roundtrips with id/1 for every encoding" do
      for encoding <- Encoding.all() do
        assert encoding |> Encoding.id() |> Encoding.from_id() == encoding
      end
    end
  end
end
