defmodule Charset.Encoding.CodePointTest do
  use ExUnit.Case, async: true

  import Charset.Encoding.CodePoint

  doctest Charset.Encoding.CodePoint

  describe "is_code_point/1" do
    test "rejects negative values", do: refute(is_code_point(-1))
    test "rejects values above U+10FFFF", do: refute(is_code_point(0x110000))
    test "rejects non-integers", do: refute(is_code_point("A"))
    test "accepts U+0000 (low boundary)", do: assert(is_code_point(0x0000))
    test "accepts U+10FFFF (high boundary)", do: assert(is_code_point(0x10FFFF))
  end

  describe "is_bmp/1" do
    test "true for U+0000 (low boundary)", do: assert(is_bmp(0x0000))
    test "true for U+D800 (surrogates are in the BMP)", do: assert(is_bmp(0xD800))
    test "true for U+FFFF (high boundary)", do: assert(is_bmp(0xFFFF))
    test "false for U+10000 (first code point above the BMP)", do: refute(is_bmp(0x10000))
    test "false for U+10FFFF (max code point)", do: refute(is_bmp(0x10FFFF))
  end

  describe "is_surrogate/1" do
    test "false for U+D7FF (just before the range)", do: refute(is_surrogate(0xD7FF))
    test "true for U+D800 (range start)", do: assert(is_surrogate(0xD800))
    test "true for U+DFFF (range end)", do: assert(is_surrogate(0xDFFF))
    test "false for U+E000 (just after the range)", do: refute(is_surrogate(0xE000))
  end
end
