defmodule Charset.SandboxTest do
  @moduledoc """
  The step composition itself is covered by the generator tests; these only
  pin the delegation - one happy path per sandbox flow.
  """
  use ExUnit.Case, async: true

  alias Charset.Exercise.Step
  alias Charset.Sandbox

  test "encode_utf8/1 returns the sandbox layout (unpadded binary, no useful-bit-count)" do
    steps = Sandbox.encode_utf8(0xE9)

    assert [%Step.Format{}, %Step.Binary{length: 11}, %Step.BitGroups{}, %Step.HexBytes{}] =
             steps

    refute Enum.any?(steps, &match?(%Step.UsefulBitCount{}, &1))
  end

  test "decode_utf8/2 ends at the code point" do
    assert %Step.CodePointEntry{expected: 0xE9} =
             List.last(Sandbox.decode_utf8(<<0xC3, 0xA9>>, 0xE9))
  end

  test "encode_utf16/2 keeps the endianness step (sandbox explanation panel)" do
    assert [%Step.Endianness{expected: :little} | _rest] = Sandbox.encode_utf16(0xE9, :little)
  end

  test "decode_utf16/3 ends at the code point" do
    assert %Step.CodePointEntry{expected: 0x1F389} =
             List.last(Sandbox.decode_utf16(<<0xD8, 0x3C, 0xDF, 0x89>>, 0x1F389, :big))
  end

  test "encode_utf32/2 keeps the endianness step" do
    assert [%Step.Endianness{expected: :big} | _rest] = Sandbox.encode_utf32(0x1F389, :big)
  end

  test "decode_utf32/3 ends at the code point" do
    assert %Step.CodePointEntry{expected: 0xE9} =
             List.last(Sandbox.decode_utf32(<<0x00, 0x00, 0x00, 0xE9>>, 0xE9, :big))
  end

  test "windows-1252 and latin1 flows are single-byte" do
    assert [%Step.Binary{length: 8}, %Step.HexBytes{expected: [0x80]}] =
             Sandbox.encode_windows1252(0x20AC)

    assert [%Step.Binary{length: 8}, %Step.CodePointEntry{expected: 0x20AC}] =
             Sandbox.decode_windows1252(<<0x80>>, 0x20AC)

    assert [%Step.Binary{length: 8}, %Step.HexBytes{expected: [0xE9]}] =
             Sandbox.encode_latin1(0xE9)

    assert [%Step.Binary{length: 8}, %Step.CodePointEntry{expected: 0xE9}] =
             Sandbox.decode_latin1(<<0xE9>>)
  end
end
