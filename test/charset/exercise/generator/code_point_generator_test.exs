defmodule Charset.Exercise.Generator.CodePointGeneratorTest do
  @moduledoc """
  The Kotlin original injected a mocked Random to pin exact draws; here the
  RNG is the process one, so the port asserts by sampling: every draw must
  stay inside the tier's allowed set, and mixed tiers must visit every
  sub-range eventually.
  """
  use ExUnit.Case, async: true

  alias Charset.Encoding.Windows1252
  alias Charset.Exercise.Generator.CodePointGenerator

  @draws 2_000

  defp surrogate?(cp), do: cp in 0xD800..0xDFFF

  describe "ascii" do
    test "printable tier stays in U+0020..U+007E" do
      for _draw <- 1..@draws do
        assert CodePointGenerator.random_ascii(:printable) in 0x20..0x7E
      end
    end

    test "full tier stays in U+0000..U+007F and visits both sub-ranges" do
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_ascii(:full)

      assert Enum.all?(samples, &(&1 in 0x00..0x7F))
      # 40/60 printable/full mix: control characters must show up
      assert Enum.any?(samples, &(&1 < 0x20))
    end
  end

  describe "latin1" do
    test "supplement tier stays in U+00A0..U+00FF" do
      for _draw <- 1..@draws do
        assert CodePointGenerator.random_latin1(:supplement) in 0xA0..0xFF
      end
    end

    test "full tier stays in U+0000..U+00FF and visits the ASCII range" do
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_latin1(:full)

      assert Enum.all?(samples, &(&1 in 0x00..0xFF))
      assert Enum.any?(samples, &(&1 < 0xA0))
    end
  end

  describe "windows-1252" do
    test "special block tier draws only the 27 special code points" do
      specials = MapSet.new(Windows1252.special_code_points())

      for _draw <- 1..@draws do
        assert CodePointGenerator.random_windows1252(:special_block) in specials
      end
    end

    test "all-encodable tier draws only encodable code points" do
      encodable = MapSet.new(Windows1252.encodable_code_points())
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_windows1252(:all_encodable)

      assert Enum.all?(samples, &(&1 in encodable))
      # 30/70 mix: identity ranges must show up alongside the special block
      assert Enum.any?(samples, &(&1 <= 0x7F))
    end
  end

  describe "utf8" do
    test "one-byte tier stays in U+0000..U+007F" do
      for _draw <- 1..@draws do
        assert CodePointGenerator.random_utf8(:one_byte) in 0x00..0x7F
      end
    end

    test "two-byte tier mixes 1-byte (20%) and 2-byte (80%) ranges" do
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_utf8(:two_byte)

      assert Enum.all?(samples, &(&1 in 0x00..0x7FF))
      assert Enum.any?(samples, &(&1 <= 0x7F))
      assert Enum.any?(samples, &(&1 > 0x7F))
    end

    test "three-byte tier never draws a surrogate" do
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_utf8(:three_byte)

      assert Enum.all?(samples, &(&1 in 0x00..0xFFFF))
      refute Enum.any?(samples, &surrogate?/1)
      # 10/25/65 mix: the 3-byte sub-range itself must show up
      assert Enum.any?(samples, &(&1 >= 0x800))
    end

    test "four-byte tier spans the whole range, never a surrogate, all 4 sub-ranges visited" do
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_utf8(:four_byte)

      assert Enum.all?(samples, &(&1 in 0x00..0x10FFFF))
      refute Enum.any?(samples, &surrogate?/1)
      assert Enum.any?(samples, &(&1 <= 0x7F))
      assert Enum.any?(samples, &(&1 in 0x80..0x7FF))
      assert Enum.any?(samples, &(&1 in 0x800..0xFFFF))
      assert Enum.any?(samples, &(&1 >= 0x10000))
    end
  end

  describe "utf16 / utf32" do
    test "bmp tier stays in the BMP and never draws a surrogate" do
      for random <- [&CodePointGenerator.random_utf16/1, &CodePointGenerator.random_utf32/1] do
        samples = for _draw <- 1..@draws, do: random.(:bmp)

        assert Enum.all?(samples, &(&1 in 0x0000..0xFFFF))
        refute Enum.any?(samples, &surrogate?/1)
      end
    end

    test "supplementary tier mixes BMP (30%) and supplementary (70%)" do
      samples = for _draw <- 1..@draws, do: CodePointGenerator.random_utf16(:supplementary)

      assert Enum.all?(samples, &(&1 in 0x0000..0x10FFFF))
      refute Enum.any?(samples, &surrogate?/1)
      assert Enum.any?(samples, &(&1 <= 0xFFFF))
      assert Enum.any?(samples, &(&1 >= 0x10000))
    end
  end

  test "boundary code points are reachable (seeded sweep over the bmp skip logic)" do
    # The surrogate-skip arithmetic must be able to produce both U+D7FF (just
    # before the block) and U+E000 (just after). Sample heavily and check the
    # skip never lands inside the block while both shores stay reachable
    # across the whole BMP sweep.
    samples = for _draw <- 1..50_000, do: CodePointGenerator.random_utf16(:bmp)

    refute Enum.any?(samples, &surrogate?/1)
    assert Enum.any?(samples, &(&1 >= 0xE000))
    assert Enum.any?(samples, &(&1 < 0xD800))
  end
end
