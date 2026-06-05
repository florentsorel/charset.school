defmodule Charset.Encoding do
  @moduledoc """
  The 8 supported encodings, identified by atoms.

  The string ids (`"utf-16be"`, ...) are the stable wire/DB identifiers,
  shared with the old implementation on `main` (`Encoding.id` in Kotlin).
  """

  @type t ::
          :ascii
          | :latin1
          | :windows1252
          | :utf8
          | :utf16be
          | :utf16le
          | :utf32be
          | :utf32le

  @type endian :: :big | :little

  @ids %{
    ascii: "ascii",
    latin1: "latin1",
    windows1252: "windows-1252",
    utf8: "utf-8",
    utf16be: "utf-16be",
    utf16le: "utf-16le",
    utf32be: "utf-32be",
    utf32le: "utf-32le"
  }

  @encodings Map.keys(@ids)

  defguard is_encoding(encoding) when encoding in @encodings

  @spec all() :: [t()]
  def all do
    [:ascii, :latin1, :windows1252, :utf8, :utf16be, :utf16le, :utf32be, :utf32le]
  end

  @doc """
  The stable string id of an encoding.

      iex> Charset.Encoding.id(:utf16be)
      "utf-16be"
  """
  @spec id(t()) :: String.t()
  def id(encoding) when is_encoding(encoding), do: @ids[encoding]

  @doc """
  Resolves a stable string id back to its encoding, nil when unknown.

      iex> Charset.Encoding.from_id("windows-1252")
      :windows1252

      iex> Charset.Encoding.from_id("invalid-encoding")
      nil
  """
  @spec from_id(String.t()) :: t() | nil
  def from_id(id) when is_binary(id) do
    Enum.find(all(), fn encoding -> @ids[encoding] == id end)
  end

  @doc """
  Byte order of the multi-byte encodings. Matches the bitstring modifiers
  (`::16-big` / `::16-little`) used by the codec.

      iex> Charset.Encoding.endianness(:utf32le)
      :little
  """
  @spec endianness(t()) :: endian()
  def endianness(:utf16be), do: :big
  def endianness(:utf16le), do: :little
  def endianness(:utf32be), do: :big
  def endianness(:utf32le), do: :little
end
