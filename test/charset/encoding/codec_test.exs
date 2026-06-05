defmodule Charset.Encoding.CodecTest do
  @moduledoc """
  Port of the Kotlin `CodecTest` from `main` - same cases, same labels, same
  asserted messages. The exhaustive sweeps live in `codec_exhaustive_test.exs`.
  """
  use ExUnit.Case, async: true

  alias Charset.Encoding.Codec
  alias Charset.Encoding.DecodeError
  alias Charset.Encoding.EncodeError

  doctest Charset.Encoding.Codec

  describe "encode/2 ascii" do
    test "U+0000 (NUL) -> 0x00 (low boundary)",
      do: assert(Codec.encode!(0x0000, :ascii) == <<0x00>>)

    test "U+0041 (A) -> 0x41", do: assert(Codec.encode!(0x41, :ascii) == <<0x41>>)

    test "U+007F (DEL) -> 0x7F (high boundary)",
      do: assert(Codec.encode!(0x7F, :ascii) == <<0x7F>>)

    test "U+0080 (PAD) errors, first code point outside ASCII" do
      assert {:error, %EncodeError{encoding: :ascii, reason: :out_of_range}} =
               Codec.encode(0x80, :ascii)
    end

    test "U+00E9 (é) errors, Latin-1, not representable in ASCII" do
      assert {:error, error} = Codec.encode(0xE9, :ascii)
      assert Exception.message(error) == "Cannot encode U+00E9 in ascii: value exceeds U+007F"
    end
  end

  describe "encode/2 latin1" do
    test "U+0000 (NUL) -> 0x00 (low boundary)",
      do: assert(Codec.encode!(0x0000, :latin1) == <<0x00>>)

    test "U+0080 (PAD) -> 0x80 (first code point outside ASCII)",
      do: assert(Codec.encode!(0x80, :latin1) == <<0x80>>)

    test "U+00FF (ÿ) -> 0xFF (high boundary)",
      do: assert(Codec.encode!(0xFF, :latin1) == <<0xFF>>)

    test "U+00E9 (é) -> 0xE9 (canonical Latin-1 char)",
      do: assert(Codec.encode!(0xE9, :latin1) == <<0xE9>>)

    test "U+0100 (Ā) errors, first code point outside Latin-1" do
      assert {:error, error} = Codec.encode(0x100, :latin1)
      assert %EncodeError{encoding: :latin1, reason: :out_of_range} = error
      assert Exception.message(error) == "Cannot encode U+0100 in latin1: value exceeds U+00FF"
    end
  end

  describe "encode/2 windows-1252" do
    test "U+0000 (NUL) -> 0x00 (low boundary)",
      do: assert(Codec.encode!(0x0000, :windows1252) == <<0x00>>)

    test "U+0041 (A) -> 0x41 (ASCII identity)",
      do: assert(Codec.encode!(0x41, :windows1252) == <<0x41>>)

    test "U+00E9 (é) -> 0xE9 (Latin-1 identity)",
      do: assert(Codec.encode!(0xE9, :windows1252) == <<0xE9>>)

    test "U+00A0 (NBSP) -> 0xA0 (above the special block)",
      do: assert(Codec.encode!(0xA0, :windows1252) == <<0xA0>>)

    test "U+00FF (ÿ) -> 0xFF (high boundary)",
      do: assert(Codec.encode!(0xFF, :windows1252) == <<0xFF>>)

    test "U+20AC (€) -> 0x80 (the marquee Windows-1252 character)",
      do: assert(Codec.encode!(0x20AC, :windows1252) == <<0x80>>)

    test "U+0152 (Œ) -> 0x8C", do: assert(Codec.encode!(0x0152, :windows1252) == <<0x8C>>)
    test "U+0153 (œ) -> 0x9C", do: assert(Codec.encode!(0x0153, :windows1252) == <<0x9C>>)
    test "U+2014 (em dash) -> 0x97", do: assert(Codec.encode!(0x2014, :windows1252) == <<0x97>>)
    test "U+2122 (trademark) -> 0x99", do: assert(Codec.encode!(0x2122, :windows1252) == <<0x99>>)

    test "U+0080 (PAD) errors - byte 0x80 maps to € (U+20AC), not U+0080" do
      assert {:error, %EncodeError{reason: :not_representable}} = Codec.encode(0x80, :windows1252)
    end

    test "U+0081 errors - unmapped (byte 0x81 is one of the 5 unassigned)" do
      assert {:error, error} = Codec.encode(0x81, :windows1252)

      assert Exception.message(error) ==
               "Cannot encode U+0081 in windows-1252: not representable in Windows-1252"
    end

    test "U+009D errors - another unassigned byte" do
      assert {:error, %EncodeError{reason: :not_representable}} = Codec.encode(0x9D, :windows1252)
    end

    test "U+0100 (Ā) errors - above Latin-1, not in special mappings" do
      assert {:error, %EncodeError{reason: :not_representable}} =
               Codec.encode(0x100, :windows1252)
    end

    test "U+1F600 (emoji) errors - supplementary plane never representable" do
      assert {:error, %EncodeError{reason: :not_representable}} =
               Codec.encode(0x1F600, :windows1252)
    end

    test "U+D800 (first surrogate) errors" do
      assert {:error, %EncodeError{reason: :not_representable}} =
               Codec.encode(0xD800, :windows1252)
    end
  end

  describe "encode/2 utf-8" do
    test "U+0000 (NUL) -> 0x00 (low boundary)",
      do: assert(Codec.encode!(0x0000, :utf8) == <<0x00>>)

    test "U+0041 (A) -> 0x41 (ASCII char encoded identically)",
      do: assert(Codec.encode!(0x41, :utf8) == <<0x41>>)

    test "U+007F (DEL) -> 0x7F (last code point in 1-byte sequence)",
      do: assert(Codec.encode!(0x7F, :utf8) == <<0x7F>>)

    test "U+0080 (PAD) -> 0xC2 0x80 (first code point in 2-byte sequence)",
      do: assert(Codec.encode!(0x80, :utf8) == <<0xC2, 0x80>>)

    test "U+07FF -> 0xDF 0xBF (last code point in 2-byte sequence)",
      do: assert(Codec.encode!(0x7FF, :utf8) == <<0xDF, 0xBF>>)

    test "U+0800 -> 0xE0 0xA0 0x80 (first code point in 3-byte sequence)",
      do: assert(Codec.encode!(0x800, :utf8) == <<0xE0, 0xA0, 0x80>>)

    test "U+4E2D (中) -> 0xE4 0xB8 0xAD (a common Chinese character)",
      do: assert(Codec.encode!(0x4E2D, :utf8) == <<0xE4, 0xB8, 0xAD>>)

    test "U+FFFF -> 0xEF 0xBF 0xBF (last code point in 3-byte sequence)",
      do: assert(Codec.encode!(0xFFFF, :utf8) == <<0xEF, 0xBF, 0xBF>>)

    test "U+10000 -> 0xF0 0x90 0x80 0x80 (first code point in 4-byte sequence)",
      do: assert(Codec.encode!(0x10000, :utf8) == <<0xF0, 0x90, 0x80, 0x80>>)

    test "U+10FFFF -> 0xF4 0x8F 0xBF 0xBF (high boundary)",
      do: assert(Codec.encode!(0x10FFFF, :utf8) == <<0xF4, 0x8F, 0xBF, 0xBF>>)

    test "U+00E9 (é) -> 0xC3 0xA9", do: assert(Codec.encode!(0xE9, :utf8) == <<0xC3, 0xA9>>)

    test "U+1F600 (emoji) -> 0xF0 0x9F 0x98 0x80",
      do: assert(Codec.encode!(0x1F600, :utf8) == <<0xF0, 0x9F, 0x98, 0x80>>)

    test "U+D800 (first surrogate) errors" do
      assert {:error, error} = Codec.encode(0xD800, :utf8)
      assert %EncodeError{encoding: :utf8, reason: :surrogate} = error

      assert Exception.message(error) ==
               "Cannot encode U+D800 in utf-8: surrogate not encodable in UTF-8"
    end

    test "U+DFFF (last surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xDFFF, :utf8)
    end
  end

  describe "encode/2 utf-16 be" do
    test "U+0000 (NUL) -> 0x00 0x00 (low boundary)",
      do: assert(Codec.encode!(0x0000, :utf16be) == <<0x00, 0x00>>)

    test "U+0041 (A) -> 0x00 0x41 (ASCII char in 16 bits)",
      do: assert(Codec.encode!(0x41, :utf16be) == <<0x00, 0x41>>)

    test "U+00E9 (é) -> 0x00 0xE9", do: assert(Codec.encode!(0xE9, :utf16be) == <<0x00, 0xE9>>)
    test "U+4E2D (中) -> 0x4E 0x2D", do: assert(Codec.encode!(0x4E2D, :utf16be) == <<0x4E, 0x2D>>)

    test "U+FFFF (non-char) -> 0xFF 0xFF (BMP max)",
      do: assert(Codec.encode!(0xFFFF, :utf16be) == <<0xFF, 0xFF>>)

    test "U+10000 -> 0xD8 0x00 0xDC 0x00 (first supplementary, surrogate pair)",
      do: assert(Codec.encode!(0x10000, :utf16be) == <<0xD8, 0x00, 0xDC, 0x00>>)

    test "U+1F600 (emoji) -> 0xD8 0x3D 0xDE 0x00 (emoji surrogate pair)",
      do: assert(Codec.encode!(0x1F600, :utf16be) == <<0xD8, 0x3D, 0xDE, 0x00>>)

    test "U+10FFFF (non-char) -> 0xDB 0xFF 0xDF 0xFF (max code point)",
      do: assert(Codec.encode!(0x10FFFF, :utf16be) == <<0xDB, 0xFF, 0xDF, 0xFF>>)

    test "U+D800 (first surrogate) errors" do
      assert {:error, error} = Codec.encode(0xD800, :utf16be)
      assert %EncodeError{encoding: :utf16be, reason: :surrogate} = error

      assert Exception.message(error) ==
               "Cannot encode U+D800 in utf-16be: surrogate not encodable standalone"
    end

    test "U+DFFF (last surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xDFFF, :utf16be)
    end
  end

  describe "encode/2 utf-16 le" do
    test "U+0000 (NUL) -> 0x00 0x00 (low boundary, palindromic)",
      do: assert(Codec.encode!(0x0000, :utf16le) == <<0x00, 0x00>>)

    test "U+0041 (A) -> 0x41 0x00 (bytes swapped vs BE)",
      do: assert(Codec.encode!(0x41, :utf16le) == <<0x41, 0x00>>)

    test "U+00E9 (é) -> 0xE9 0x00", do: assert(Codec.encode!(0xE9, :utf16le) == <<0xE9, 0x00>>)
    test "U+4E2D (中) -> 0x2D 0x4E", do: assert(Codec.encode!(0x4E2D, :utf16le) == <<0x2D, 0x4E>>)

    test "U+FFFF (non-char) -> 0xFF 0xFF (BMP max, palindromic)",
      do: assert(Codec.encode!(0xFFFF, :utf16le) == <<0xFF, 0xFF>>)

    test "U+10000 -> 0x00 0xD8 0x00 0xDC (first supplementary, surrogate pair LE)",
      do: assert(Codec.encode!(0x10000, :utf16le) == <<0x00, 0xD8, 0x00, 0xDC>>)

    test "U+1F600 (emoji) -> 0x3D 0xD8 0x00 0xDE",
      do: assert(Codec.encode!(0x1F600, :utf16le) == <<0x3D, 0xD8, 0x00, 0xDE>>)

    test "U+10FFFF (non-char) -> 0xFF 0xDB 0xFF 0xDF (max code point)",
      do: assert(Codec.encode!(0x10FFFF, :utf16le) == <<0xFF, 0xDB, 0xFF, 0xDF>>)

    test "U+D800 (first surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xD800, :utf16le)
    end

    test "U+DFFF (last surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xDFFF, :utf16le)
    end
  end

  describe "encode/2 utf-32 be" do
    test "U+0000 (NUL) -> 0x00 0x00 0x00 0x00 (low boundary)",
      do: assert(Codec.encode!(0x0000, :utf32be) == <<0x00, 0x00, 0x00, 0x00>>)

    test "U+0041 (A) -> 0x00 0x00 0x00 0x41 (ASCII char in 32 bits)",
      do: assert(Codec.encode!(0x41, :utf32be) == <<0x00, 0x00, 0x00, 0x41>>)

    test "U+00E9 (é) -> 0x00 0x00 0x00 0xE9",
      do: assert(Codec.encode!(0xE9, :utf32be) == <<0x00, 0x00, 0x00, 0xE9>>)

    test "U+4E2D (中) -> 0x00 0x00 0x4E 0x2D",
      do: assert(Codec.encode!(0x4E2D, :utf32be) == <<0x00, 0x00, 0x4E, 0x2D>>)

    test "U+FFFF (non-char) -> 0x00 0x00 0xFF 0xFF (BMP max)",
      do: assert(Codec.encode!(0xFFFF, :utf32be) == <<0x00, 0x00, 0xFF, 0xFF>>)

    test "U+10000 -> 0x00 0x01 0x00 0x00 (first supplementary)",
      do: assert(Codec.encode!(0x10000, :utf32be) == <<0x00, 0x01, 0x00, 0x00>>)

    test "U+1F600 (emoji) -> 0x00 0x01 0xF6 0x00",
      do: assert(Codec.encode!(0x1F600, :utf32be) == <<0x00, 0x01, 0xF6, 0x00>>)

    test "U+10FFFF (non-char) -> 0x00 0x10 0xFF 0xFF (max code point)",
      do: assert(Codec.encode!(0x10FFFF, :utf32be) == <<0x00, 0x10, 0xFF, 0xFF>>)

    test "U+D800 (first surrogate) errors" do
      assert {:error, error} = Codec.encode(0xD800, :utf32be)
      assert %EncodeError{encoding: :utf32be, reason: :surrogate} = error

      assert Exception.message(error) ==
               "Cannot encode U+D800 in utf-32be: surrogate not encodable in UTF-32"
    end

    test "U+DFFF (last surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xDFFF, :utf32be)
    end
  end

  describe "encode/2 utf-32 le" do
    test "U+0000 (NUL) -> 0x00 0x00 0x00 0x00 (low boundary, palindromic)",
      do: assert(Codec.encode!(0x0000, :utf32le) == <<0x00, 0x00, 0x00, 0x00>>)

    test "U+0041 (A) -> 0x41 0x00 0x00 0x00 (bytes fully reversed vs BE)",
      do: assert(Codec.encode!(0x41, :utf32le) == <<0x41, 0x00, 0x00, 0x00>>)

    test "U+00E9 (é) -> 0xE9 0x00 0x00 0x00",
      do: assert(Codec.encode!(0xE9, :utf32le) == <<0xE9, 0x00, 0x00, 0x00>>)

    test "U+4E2D (中) -> 0x2D 0x4E 0x00 0x00",
      do: assert(Codec.encode!(0x4E2D, :utf32le) == <<0x2D, 0x4E, 0x00, 0x00>>)

    test "U+FFFF (non-char) -> 0xFF 0xFF 0x00 0x00 (BMP max)",
      do: assert(Codec.encode!(0xFFFF, :utf32le) == <<0xFF, 0xFF, 0x00, 0x00>>)

    test "U+10000 -> 0x00 0x00 0x01 0x00 (first supplementary)",
      do: assert(Codec.encode!(0x10000, :utf32le) == <<0x00, 0x00, 0x01, 0x00>>)

    test "U+1F600 (emoji) -> 0x00 0xF6 0x01 0x00",
      do: assert(Codec.encode!(0x1F600, :utf32le) == <<0x00, 0xF6, 0x01, 0x00>>)

    test "U+10FFFF (non-char) -> 0xFF 0xFF 0x10 0x00 (max code point)",
      do: assert(Codec.encode!(0x10FFFF, :utf32le) == <<0xFF, 0xFF, 0x10, 0x00>>)

    test "U+D800 (first surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xD800, :utf32le)
    end

    test "U+DFFF (last surrogate) errors" do
      assert {:error, %EncodeError{reason: :surrogate}} = Codec.encode(0xDFFF, :utf32le)
    end
  end

  describe "decode/2 ascii" do
    test "0x00 -> U+0000 (NUL, low boundary)",
      do: assert(Codec.decode!(<<0x00>>, :ascii) == 0x0000)

    test "0x41 -> U+0041 (A)", do: assert(Codec.decode!(<<0x41>>, :ascii) == 0x41)

    test "0x7F -> U+007F (DEL, high boundary)",
      do: assert(Codec.decode!(<<0x7F>>, :ascii) == 0x7F)

    test "0x80 errors, high bit set, not ASCII" do
      assert {:error, error} = Codec.decode(<<0x80>>, :ascii)
      assert %DecodeError{encoding: :ascii, reason: :high_bit_set} = error
      assert Exception.message(error) == "Cannot decode [80] in ascii: high bit set, not ASCII"
    end

    test "0xE9 errors, Latin-1 byte not ASCII" do
      assert {:error, %DecodeError{reason: :high_bit_set}} = Codec.decode(<<0xE9>>, :ascii)
    end

    test "0xFF errors, high byte not ASCII" do
      assert {:error, %DecodeError{reason: :high_bit_set}} = Codec.decode(<<0xFF>>, :ascii)
    end

    test "[] errors, expected 1 byte got 0" do
      assert {:error, error} = Codec.decode(<<>>, :ascii)
      assert %DecodeError{reason: :bad_length} = error

      assert Exception.message(error) ==
               "Cannot decode [] in ascii: expected exactly 1 byte, got 0"
    end

    test "[41 42] errors, expected 1 byte got 2" do
      assert {:error, error} = Codec.decode(<<0x41, 0x42>>, :ascii)

      assert Exception.message(error) ==
               "Cannot decode [41 42] in ascii: expected exactly 1 byte, got 2"
    end
  end

  describe "decode/2 latin1" do
    test "0x41 -> U+0041 (A, ASCII)", do: assert(Codec.decode!(<<0x41>>, :latin1) == 0x41)

    test "0x7F -> U+007F (DEL, ASCII boundary)",
      do: assert(Codec.decode!(<<0x7F>>, :latin1) == 0x7F)

    test "0x00 -> U+0000 (NUL, low boundary)",
      do: assert(Codec.decode!(<<0x00>>, :latin1) == 0x0000)

    test "0x80 -> U+0080 (PAD, first code point outside ASCII)",
      do: assert(Codec.decode!(<<0x80>>, :latin1) == 0x80)

    test "0xFF -> U+00FF (ÿ, high boundary)", do: assert(Codec.decode!(<<0xFF>>, :latin1) == 0xFF)

    test "0xE9 -> U+00E9 (é, canonical Latin-1 char)",
      do: assert(Codec.decode!(<<0xE9>>, :latin1) == 0xE9)

    test "[41 42] errors, expected 1 byte got 2" do
      assert {:error, error} = Codec.decode(<<0x41, 0x42>>, :latin1)

      assert Exception.message(error) ==
               "Cannot decode [41 42] in latin1: expected exactly 1 byte, got 2"
    end

    test "[] errors, expected 1 byte got 0" do
      assert {:error, error} = Codec.decode(<<>>, :latin1)

      assert Exception.message(error) ==
               "Cannot decode [] in latin1: expected exactly 1 byte, got 0"
    end
  end

  describe "decode/2 windows-1252" do
    test "0x00 -> U+0000 (NUL, low boundary)",
      do: assert(Codec.decode!(<<0x00>>, :windows1252) == 0x0000)

    test "0x41 -> U+0041 (A, ASCII identity)",
      do: assert(Codec.decode!(<<0x41>>, :windows1252) == 0x41)

    test "0x7F -> U+007F (DEL, last identity-ASCII byte)",
      do: assert(Codec.decode!(<<0x7F>>, :windows1252) == 0x7F)

    test "0xA0 -> U+00A0 (NBSP, first byte above the special block)",
      do: assert(Codec.decode!(<<0xA0>>, :windows1252) == 0xA0)

    test "0xE9 -> U+00E9 (é, Latin-1 identity)",
      do: assert(Codec.decode!(<<0xE9>>, :windows1252) == 0xE9)

    test "0xFF -> U+00FF (ÿ, high boundary)",
      do: assert(Codec.decode!(<<0xFF>>, :windows1252) == 0xFF)

    test "0x80 -> U+20AC (€, marquee character)",
      do: assert(Codec.decode!(<<0x80>>, :windows1252) == 0x20AC)

    test "0x8C -> U+0152 (Œ)", do: assert(Codec.decode!(<<0x8C>>, :windows1252) == 0x0152)
    test "0x97 -> U+2014 (em dash)", do: assert(Codec.decode!(<<0x97>>, :windows1252) == 0x2014)
    test "0x99 -> U+2122 (trademark)", do: assert(Codec.decode!(<<0x99>>, :windows1252) == 0x2122)
    test "0x9C -> U+0153 (œ)", do: assert(Codec.decode!(<<0x9C>>, :windows1252) == 0x0153)

    test "0x81 errors, unassigned byte" do
      assert {:error, error} = Codec.decode(<<0x81>>, :windows1252)
      assert %DecodeError{reason: :unassigned_byte} = error

      assert Exception.message(error) ==
               "Cannot decode [81] in windows-1252: byte 0x81 is unassigned in Windows-1252"
    end

    test "0x8D errors, unassigned byte" do
      assert {:error, %DecodeError{reason: :unassigned_byte}} =
               Codec.decode(<<0x8D>>, :windows1252)
    end

    test "0x8F errors, unassigned byte" do
      assert {:error, %DecodeError{reason: :unassigned_byte}} =
               Codec.decode(<<0x8F>>, :windows1252)
    end

    test "0x90 errors, unassigned byte" do
      assert {:error, %DecodeError{reason: :unassigned_byte}} =
               Codec.decode(<<0x90>>, :windows1252)
    end

    test "0x9D errors, unassigned byte" do
      assert {:error, %DecodeError{reason: :unassigned_byte}} =
               Codec.decode(<<0x9D>>, :windows1252)
    end

    test "[41 42] errors, expected 1 byte got 2" do
      assert {:error, error} = Codec.decode(<<0x41, 0x42>>, :windows1252)

      assert Exception.message(error) ==
               "Cannot decode [41 42] in windows-1252: expected exactly 1 byte, got 2"
    end

    test "[] errors, expected 1 byte got 0" do
      assert {:error, error} = Codec.decode(<<>>, :windows1252)

      assert Exception.message(error) ==
               "Cannot decode [] in windows-1252: expected exactly 1 byte, got 0"
    end
  end

  describe "decode/2 utf-8" do
    test "[00] -> U+0000 (NUL, low boundary)",
      do: assert(Codec.decode!(<<0x00>>, :utf8) == 0x0000)

    test "[41] -> U+0041 (A)", do: assert(Codec.decode!(<<0x41>>, :utf8) == 0x41)

    test "[7F] -> U+007F (DEL, last 1-byte code point)",
      do: assert(Codec.decode!(<<0x7F>>, :utf8) == 0x7F)

    test "[C2 80] -> U+0080 (PAD, first 2-byte code point)",
      do: assert(Codec.decode!(<<0xC2, 0x80>>, :utf8) == 0x80)

    test "[C3 A9] -> U+00E9 (é, canary)", do: assert(Codec.decode!(<<0xC3, 0xA9>>, :utf8) == 0xE9)

    test "[DF BF] -> U+07FF (last 2-byte code point)",
      do: assert(Codec.decode!(<<0xDF, 0xBF>>, :utf8) == 0x7FF)

    test "[E0 A0 80] -> U+0800 (first 3-byte code point)",
      do: assert(Codec.decode!(<<0xE0, 0xA0, 0x80>>, :utf8) == 0x800)

    test "[E4 B8 AD] -> U+4E2D (中)",
      do: assert(Codec.decode!(<<0xE4, 0xB8, 0xAD>>, :utf8) == 0x4E2D)

    test "[EF BF BF] -> U+FFFF (last 3-byte code point, BMP max)",
      do: assert(Codec.decode!(<<0xEF, 0xBF, 0xBF>>, :utf8) == 0xFFFF)

    test "[F0 90 80 80] -> U+10000 (first 4-byte code point, first supplementary)",
      do: assert(Codec.decode!(<<0xF0, 0x90, 0x80, 0x80>>, :utf8) == 0x10000)

    test "[F0 9F 98 80] -> U+1F600 (emoji)",
      do: assert(Codec.decode!(<<0xF0, 0x9F, 0x98, 0x80>>, :utf8) == 0x1F600)

    test "[F4 8F BF BF] -> U+10FFFF (high boundary)",
      do: assert(Codec.decode!(<<0xF4, 0x8F, 0xBF, 0xBF>>, :utf8) == 0x10FFFF)

    test "[] errors, empty input" do
      assert {:error, error} = Codec.decode(<<>>, :utf8)
      assert %DecodeError{reason: :empty_input} = error
      assert Exception.message(error) == "Cannot decode [] in utf-8: empty input"
    end

    test "[80] errors, continuation byte at start" do
      assert {:error, error} = Codec.decode(<<0x80>>, :utf8)
      assert %DecodeError{reason: :continuation_as_leader} = error

      assert Exception.message(error) ==
               "Cannot decode [80] in utf-8: byte 0x80 is a continuation byte, not a valid leader"
    end

    test "[BF] errors, high continuation byte at start" do
      assert {:error, %DecodeError{reason: :continuation_as_leader}} =
               Codec.decode(<<0xBF>>, :utf8)
    end

    test "[F8 80 80 80 80] errors, 5-byte leader is invalid" do
      assert {:error, error} = Codec.decode(<<0xF8, 0x80, 0x80, 0x80, 0x80>>, :utf8)
      assert %DecodeError{reason: :invalid_leader} = error

      assert Exception.message(error) ==
               "Cannot decode [F8 80 80 80 80] in utf-8: invalid leading byte 0xF8"
    end

    test "[FF] errors, 0xFF is never valid in UTF-8" do
      assert {:error, %DecodeError{reason: :invalid_leader}} = Codec.decode(<<0xFF>>, :utf8)
    end

    test "[C2] errors, expected 2 bytes got 1" do
      assert {:error, error} = Codec.decode(<<0xC2>>, :utf8)
      assert %DecodeError{reason: {:bad_length, 2}} = error

      assert Exception.message(error) ==
               "Cannot decode [C2] in utf-8: expected 2 bytes for leading byte 0xC2, got 1"
    end

    test "[E0 A0] errors, 3-byte sequence with only 1 continuation" do
      assert {:error, %DecodeError{reason: {:bad_length, 3}}} =
               Codec.decode(<<0xE0, 0xA0>>, :utf8)
    end

    test "[C2 C2] errors, second byte not a continuation (10xxxxxx)" do
      assert {:error, error} = Codec.decode(<<0xC2, 0xC2>>, :utf8)
      assert %DecodeError{reason: {:invalid_continuation, 1}} = error

      assert Exception.message(error) ==
               "Cannot decode [C2 C2] in utf-8: byte 1 (0xC2) is not a valid continuation byte"
    end

    test "[C0 80] errors, overlong U+0000 in 2 bytes (must use [00])" do
      assert {:error, error} = Codec.decode(<<0xC0, 0x80>>, :utf8)
      assert %DecodeError{reason: {:overlong, 0}} = error

      assert Exception.message(error) ==
               "Cannot decode [C0 80] in utf-8: overlong encoding: U+0000 should use a shorter form"
    end

    test "[E0 80 80] errors, overlong U+0000 in 3 bytes" do
      assert {:error, %DecodeError{reason: {:overlong, 0}}} =
               Codec.decode(<<0xE0, 0x80, 0x80>>, :utf8)
    end

    test "[F0 80 80 80] errors, overlong U+0000 in 4 bytes" do
      assert {:error, %DecodeError{reason: {:overlong, 0}}} =
               Codec.decode(<<0xF0, 0x80, 0x80, 0x80>>, :utf8)
    end

    test "[ED A0 80] errors, surrogate U+D800 not decodable" do
      assert {:error, error} = Codec.decode(<<0xED, 0xA0, 0x80>>, :utf8)
      assert %DecodeError{reason: {:surrogate, 0xD800}} = error

      assert Exception.message(error) ==
               "Cannot decode [ED A0 80] in utf-8: surrogate U+D800 not a valid code point"
    end

    test "[ED BF BF] errors, surrogate U+DFFF not decodable" do
      assert {:error, %DecodeError{reason: {:surrogate, 0xDFFF}}} =
               Codec.decode(<<0xED, 0xBF, 0xBF>>, :utf8)
    end

    test "[F4 90 80 80] errors, U+110000 exceeds U+10FFFF" do
      assert {:error, error} = Codec.decode(<<0xF4, 0x90, 0x80, 0x80>>, :utf8)
      assert %DecodeError{reason: {:exceeds_max, 0x110000}} = error

      assert Exception.message(error) ==
               "Cannot decode [F4 90 80 80] in utf-8: value U+110000 exceeds U+10FFFF"
    end

    test "[F5 80 80 80] errors, leader 0xF5 produces value above U+10FFFF" do
      assert {:error, %DecodeError{reason: {:exceeds_max, _}}} =
               Codec.decode(<<0xF5, 0x80, 0x80, 0x80>>, :utf8)
    end
  end

  describe "decode/2 utf-16 be" do
    test "[00 00] -> U+0000 (NUL, low boundary)",
      do: assert(Codec.decode!(<<0x00, 0x00>>, :utf16be) == 0x0000)

    test "[00 41] -> U+0041 (A)", do: assert(Codec.decode!(<<0x00, 0x41>>, :utf16be) == 0x41)
    test "[00 E9] -> U+00E9 (é)", do: assert(Codec.decode!(<<0x00, 0xE9>>, :utf16be) == 0xE9)
    test "[4E 2D] -> U+4E2D (中)", do: assert(Codec.decode!(<<0x4E, 0x2D>>, :utf16be) == 0x4E2D)

    test "[FF FF] -> U+FFFF (BMP max)",
      do: assert(Codec.decode!(<<0xFF, 0xFF>>, :utf16be) == 0xFFFF)

    test "[D8 00 DC 00] -> U+10000 (first supplementary)",
      do: assert(Codec.decode!(<<0xD8, 0x00, 0xDC, 0x00>>, :utf16be) == 0x10000)

    test "[D8 3D DE 00] -> U+1F600 (emoji)",
      do: assert(Codec.decode!(<<0xD8, 0x3D, 0xDE, 0x00>>, :utf16be) == 0x1F600)

    test "[DB FF DF FF] -> U+10FFFF (max code point)",
      do: assert(Codec.decode!(<<0xDB, 0xFF, 0xDF, 0xFF>>, :utf16be) == 0x10FFFF)

    test "[D8 00] errors, lone high surrogate (size 2)" do
      assert {:error, error} = Codec.decode(<<0xD8, 0x00>>, :utf16be)
      assert %DecodeError{reason: {:lone_high_surrogate, 0xD800}} = error

      assert Exception.message(error) ==
               "Cannot decode [D8 00] in utf-16be: high surrogate 0xD800 requires a following low surrogate"
    end

    test "[DC 00] errors, lone low surrogate" do
      assert {:error, error} = Codec.decode(<<0xDC, 0x00>>, :utf16be)
      assert %DecodeError{reason: {:lone_low_surrogate, 0xDC00}} = error

      assert Exception.message(error) ==
               "Cannot decode [DC 00] in utf-16be: lone low surrogate 0xDC00"
    end

    test "[D8 00 00 41] errors, high surrogate not followed by low surrogate" do
      assert {:error, %DecodeError{reason: {:unpaired_high_surrogate, 0xD800, 0x41}}} =
               Codec.decode(<<0xD8, 0x00, 0x00, 0x41>>, :utf16be)
    end

    test "[D8 00 D8 00] errors, high surrogate followed by another high" do
      assert {:error, %DecodeError{reason: {:unpaired_high_surrogate, 0xD800, 0xD800}}} =
               Codec.decode(<<0xD8, 0x00, 0xD8, 0x00>>, :utf16be)
    end

    test "[] errors, expected 2 or 4 bytes got 0" do
      assert {:error, %DecodeError{reason: :bad_length}} = Codec.decode(<<>>, :utf16be)
    end

    test "[00] errors, expected 2 or 4 bytes got 1" do
      assert {:error, %DecodeError{reason: :bad_length}} = Codec.decode(<<0x00>>, :utf16be)
    end

    test "[00 00 00] errors, expected 2 or 4 bytes got 3" do
      assert {:error, %DecodeError{reason: :bad_length}} =
               Codec.decode(<<0x00, 0x00, 0x00>>, :utf16be)
    end

    test "[00 41 00 42] errors, BMP code point must be exactly 2 bytes" do
      assert {:error, error} = Codec.decode(<<0x00, 0x41, 0x00, 0x42>>, :utf16be)
      assert %DecodeError{reason: {:bmp_extra_bytes, 0x41}} = error

      assert Exception.message(error) ==
               "Cannot decode [00 41 00 42] in utf-16be: BMP code point 0x0041 must be exactly 2 bytes, got 4"
    end
  end

  describe "decode/2 utf-16 le" do
    test "[00 00] -> U+0000 (NUL, palindromic)",
      do: assert(Codec.decode!(<<0x00, 0x00>>, :utf16le) == 0x0000)

    test "[41 00] -> U+0041 (A, bytes swapped vs BE)",
      do: assert(Codec.decode!(<<0x41, 0x00>>, :utf16le) == 0x41)

    test "[2D 4E] -> U+4E2D (中)", do: assert(Codec.decode!(<<0x2D, 0x4E>>, :utf16le) == 0x4E2D)

    test "[FF FF] -> U+FFFF (BMP max, palindromic)",
      do: assert(Codec.decode!(<<0xFF, 0xFF>>, :utf16le) == 0xFFFF)

    test "[00 D8 00 DC] -> U+10000 (first supplementary, surrogate pair LE)",
      do: assert(Codec.decode!(<<0x00, 0xD8, 0x00, 0xDC>>, :utf16le) == 0x10000)

    test "[3D D8 00 DE] -> U+1F600 (emoji)",
      do: assert(Codec.decode!(<<0x3D, 0xD8, 0x00, 0xDE>>, :utf16le) == 0x1F600)

    test "[FF DB FF DF] -> U+10FFFF (max code point)",
      do: assert(Codec.decode!(<<0xFF, 0xDB, 0xFF, 0xDF>>, :utf16le) == 0x10FFFF)

    test "[00 D8] errors, lone high surrogate" do
      assert {:error, %DecodeError{reason: {:lone_high_surrogate, 0xD800}}} =
               Codec.decode(<<0x00, 0xD8>>, :utf16le)
    end

    test "[00 DC] errors, lone low surrogate" do
      assert {:error, %DecodeError{reason: {:lone_low_surrogate, 0xDC00}}} =
               Codec.decode(<<0x00, 0xDC>>, :utf16le)
    end

    test "[00 D8 41 00] errors, high surrogate not followed by low (LE)" do
      assert {:error, %DecodeError{reason: {:unpaired_high_surrogate, 0xD800, 0x41}}} =
               Codec.decode(<<0x00, 0xD8, 0x41, 0x00>>, :utf16le)
    end

    test "[] errors, expected 2 or 4 bytes got 0" do
      assert {:error, %DecodeError{reason: :bad_length}} = Codec.decode(<<>>, :utf16le)
    end

    test "[00 00 00] errors, expected 2 or 4 bytes got 3" do
      assert {:error, %DecodeError{reason: :bad_length}} =
               Codec.decode(<<0x00, 0x00, 0x00>>, :utf16le)
    end
  end

  describe "decode/2 utf-32 be" do
    test "[00 00 00 00] -> U+0000 (NUL, low boundary)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0x00, 0x00>>, :utf32be) == 0x0000)

    test "[00 00 00 41] -> U+0041 (A)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0x00, 0x41>>, :utf32be) == 0x41)

    test "[00 00 00 E9] -> U+00E9 (é)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0x00, 0xE9>>, :utf32be) == 0xE9)

    test "[00 00 4E 2D] -> U+4E2D (中)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0x4E, 0x2D>>, :utf32be) == 0x4E2D)

    test "[00 00 FF FF] -> U+FFFF (BMP max)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0xFF, 0xFF>>, :utf32be) == 0xFFFF)

    test "[00 01 00 00] -> U+10000 (first supplementary)",
      do: assert(Codec.decode!(<<0x00, 0x01, 0x00, 0x00>>, :utf32be) == 0x10000)

    test "[00 01 F6 00] -> U+1F600 (emoji)",
      do: assert(Codec.decode!(<<0x00, 0x01, 0xF6, 0x00>>, :utf32be) == 0x1F600)

    test "[00 10 FF FF] -> U+10FFFF (max code point)",
      do: assert(Codec.decode!(<<0x00, 0x10, 0xFF, 0xFF>>, :utf32be) == 0x10FFFF)

    test "[00 00 D8 00] errors, surrogate U+D800" do
      assert {:error, error} = Codec.decode(<<0x00, 0x00, 0xD8, 0x00>>, :utf32be)
      assert %DecodeError{reason: {:surrogate, 0xD800}} = error

      assert Exception.message(error) ==
               "Cannot decode [00 00 D8 00] in utf-32be: surrogate U+D800 not a valid code point"
    end

    test "[00 00 DF FF] errors, surrogate U+DFFF" do
      assert {:error, %DecodeError{reason: {:surrogate, 0xDFFF}}} =
               Codec.decode(<<0x00, 0x00, 0xDF, 0xFF>>, :utf32be)
    end

    test "[00 11 00 00] errors, U+110000 exceeds U+10FFFF" do
      assert {:error, error} = Codec.decode(<<0x00, 0x11, 0x00, 0x00>>, :utf32be)
      assert %DecodeError{reason: {:exceeds_max, 0x110000}} = error

      assert Exception.message(error) ==
               "Cannot decode [00 11 00 00] in utf-32be: value 0x110000 exceeds U+10FFFF"
    end

    test "[FF FF FF FF] errors, far above range (catches sign-extension bug)" do
      assert {:error, %DecodeError{reason: {:exceeds_max, 0xFFFFFFFF}}} =
               Codec.decode(<<0xFF, 0xFF, 0xFF, 0xFF>>, :utf32be)
    end

    test "[] errors, expected 4 bytes got 0" do
      assert {:error, %DecodeError{reason: :bad_length}} = Codec.decode(<<>>, :utf32be)
    end

    test "[00] errors, expected 4 bytes got 1" do
      assert {:error, %DecodeError{reason: :bad_length}} = Codec.decode(<<0x00>>, :utf32be)
    end

    test "[00 00 00] errors, expected 4 bytes got 3" do
      assert {:error, %DecodeError{reason: :bad_length}} =
               Codec.decode(<<0x00, 0x00, 0x00>>, :utf32be)
    end

    test "[00 00 00 00 00] errors, expected 4 bytes got 5" do
      assert {:error, %DecodeError{reason: :bad_length}} =
               Codec.decode(<<0x00, 0x00, 0x00, 0x00, 0x00>>, :utf32be)
    end
  end

  describe "decode/2 utf-32 le" do
    test "[00 00 00 00] -> U+0000 (NUL, palindromic)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0x00, 0x00>>, :utf32le) == 0x0000)

    test "[41 00 00 00] -> U+0041 (A, bytes fully reversed)",
      do: assert(Codec.decode!(<<0x41, 0x00, 0x00, 0x00>>, :utf32le) == 0x41)

    test "[2D 4E 00 00] -> U+4E2D (中)",
      do: assert(Codec.decode!(<<0x2D, 0x4E, 0x00, 0x00>>, :utf32le) == 0x4E2D)

    test "[00 00 01 00] -> U+10000 (first supplementary)",
      do: assert(Codec.decode!(<<0x00, 0x00, 0x01, 0x00>>, :utf32le) == 0x10000)

    test "[00 F6 01 00] -> U+1F600 (emoji)",
      do: assert(Codec.decode!(<<0x00, 0xF6, 0x01, 0x00>>, :utf32le) == 0x1F600)

    test "[FF FF 10 00] -> U+10FFFF (max code point)",
      do: assert(Codec.decode!(<<0xFF, 0xFF, 0x10, 0x00>>, :utf32le) == 0x10FFFF)

    test "[00 D8 00 00] errors, surrogate U+D800" do
      assert {:error, %DecodeError{reason: {:surrogate, 0xD800}}} =
               Codec.decode(<<0x00, 0xD8, 0x00, 0x00>>, :utf32le)
    end

    test "[FF DF 00 00] errors, surrogate U+DFFF" do
      assert {:error, %DecodeError{reason: {:surrogate, 0xDFFF}}} =
               Codec.decode(<<0xFF, 0xDF, 0x00, 0x00>>, :utf32le)
    end

    test "[00 00 11 00] errors, U+110000 exceeds U+10FFFF" do
      assert {:error, %DecodeError{reason: {:exceeds_max, 0x110000}}} =
               Codec.decode(<<0x00, 0x00, 0x11, 0x00>>, :utf32le)
    end

    test "[FF FF FF FF] errors, far above range" do
      assert {:error, %DecodeError{reason: {:exceeds_max, 0xFFFFFFFF}}} =
               Codec.decode(<<0xFF, 0xFF, 0xFF, 0xFF>>, :utf32le)
    end
  end
end
