defmodule Charset.Encoding.Windows1252Test do
  use ExUnit.Case, async: true

  alias Charset.Encoding.Windows1252

  doctest Charset.Encoding.Windows1252

  test "has exactly 27 special code points, ordered by byte value" do
    specials = Windows1252.special_code_points()

    assert length(specials) == 27

    assert specials |> Enum.map(&Windows1252.to_byte/1) |> Enum.sort() ==
             Enum.map(specials, &Windows1252.to_byte/1)
  end

  test "has exactly 251 encodable code points" do
    assert length(Windows1252.encodable_code_points()) == 251
  end

  test "the 5 unassigned bytes have no reverse mapping" do
    for byte <- [0x81, 0x8D, 0x8F, 0x90, 0x9D] do
      assert Windows1252.to_code_point(byte) == nil
    end
  end

  test "forward and reverse maps are inverses" do
    for cp <- Windows1252.special_code_points() do
      assert cp |> Windows1252.to_byte() |> Windows1252.to_code_point() == cp
    end
  end
end
