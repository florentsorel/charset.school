defmodule Charset.Sandbox.CodePointLabelsTest do
  use ExUnit.Case, async: true

  alias Charset.Sandbox.CodePointLabels

  test "returns C0 mnemonics for 0x00..0x1F" do
    assert CodePointLabels.lookup(0x00) == "NUL"
    assert CodePointLabels.lookup(0x07) == "BEL"
    assert CodePointLabels.lookup(0x0A) == "LF"
    assert CodePointLabels.lookup(0x0D) == "CR"
    assert CodePointLabels.lookup(0x0F) == "SI"
    assert CodePointLabels.lookup(0x1B) == "ESC"
    assert CodePointLabels.lookup(0x1F) == "US"
  end

  test "returns SPACE for 0x20 (would otherwise render invisibly)" do
    assert CodePointLabels.lookup(0x20) == "SPACE"
  end

  test "returns DEL for 0x7F" do
    assert CodePointLabels.lookup(0x7F) == "DEL"
  end

  test "returns C1 mnemonics for 0x80..0x9F" do
    assert CodePointLabels.lookup(0x80) == "PAD"
    assert CodePointLabels.lookup(0x85) == "NEL"
    assert CodePointLabels.lookup(0x9B) == "CSI"
    assert CodePointLabels.lookup(0x9F) == "APC"
  end

  test "returns nil for printable ASCII other than space" do
    assert CodePointLabels.lookup(0x21) == nil
    assert CodePointLabels.lookup(0x41) == nil
    assert CodePointLabels.lookup(0x7E) == nil
  end

  test "returns named short labels for common format/invisible chars" do
    assert CodePointLabels.lookup(0x00A0) == "NBSP"
    assert CodePointLabels.lookup(0x00AD) == "SHY"
    assert CodePointLabels.lookup(0x200B) == "ZWSP"
    assert CodePointLabels.lookup(0x200D) == "ZWJ"
    assert CodePointLabels.lookup(0x200E) == "LRM"
    assert CodePointLabels.lookup(0x2028) == "LSEP"
    assert CodePointLabels.lookup(0x2029) == "PSEP"
    assert CodePointLabels.lookup(0x2060) == "WJ"
    assert CodePointLabels.lookup(0xFEFF) == "BOM"
  end

  test "returns PUA for Private Use Area code points" do
    assert CodePointLabels.lookup(0xE000) == "PUA"
    assert CodePointLabels.lookup(0xF389) == "PUA"
    assert CodePointLabels.lookup(0xF8FF) == "PUA"
    assert CodePointLabels.lookup(0xF0000) == "PUA"
    assert CodePointLabels.lookup(0x100000) == "PUA"
  end

  test "returns NONCHAR for Unicode non-characters" do
    assert CodePointLabels.lookup(0xFDD0) == "NONCHAR"
    assert CodePointLabels.lookup(0xFDEF) == "NONCHAR"
    assert CodePointLabels.lookup(0xFFFE) == "NONCHAR"
    assert CodePointLabels.lookup(0xFFFF) == "NONCHAR"
    assert CodePointLabels.lookup(0x1FFFE) == "NONCHAR"
    assert CodePointLabels.lookup(0x10FFFF) == "NONCHAR"
  end

  test "returns COMBINING for combining marks (would render on dotted circle in isolation)" do
    assert CodePointLabels.lookup(0x0301) == "COMBINING"
    assert CodePointLabels.lookup(0x0308) == "COMBINING"
  end

  test "returns WHITESPACE for non-ASCII space separators" do
    assert CodePointLabels.lookup(0x2003) == "WHITESPACE"
    assert CodePointLabels.lookup(0x202F) == "WHITESPACE"
  end

  test "returns nil for printable letters and symbols above C1" do
    assert CodePointLabels.lookup(0xE9) == nil
    assert CodePointLabels.lookup(0x4E2D) == nil
    assert CodePointLabels.lookup(0x1F389) == nil
  end
end
