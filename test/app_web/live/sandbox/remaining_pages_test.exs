defmodule AppWeb.SandboxLive.RemainingPagesTest do
  use AppWeb.ConnCase, async: true

  import Phoenix.LiveViewTest

  describe "utf-16 encode" do
    test "default example: U+1F389 little endian, surrogate pair 3C D8 89 DF", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-16")

      assert html =~ "U+1F389"
      assert html =~ "0x3C 0xD8 0x89 0xDF"
      assert html =~ "2 code units · surrogate pair"
      # surrogate split step (0x1F389 - 0x10000 = 0xF389 over 20 bits)
      assert html =~ "0000111100 | 1110001001"
    end

    test "switching to big endian reorders the bytes", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-16?input=U%2B1F389&endian=big")
      assert html =~ "0xD8 0x3C 0xDF 0x89"
    end

    test "BMP code point uses a single code unit and no split step", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-16?input=U%2B00E9&endian=big")

      assert html =~ "1 code unit · BMP"
      assert html =~ "0x00 0xE9"
      refute html =~ "Split for the surrogates"
    end

    test "endian radio change patches the URL", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/sandbox/encode/utf-16")

      html = view |> element("form") |> render_change(%{"input" => "U+1F389", "endian" => "big"})
      assert_patch(view)
      assert html =~ "0xD8 0x3C 0xDF 0x89"
    end
  end

  describe "utf-16 decode" do
    test "default example: 3C D8 89 DF little endian -> U+1F389", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-16")

      assert html =~ "U+1F389"
      assert html =~ "🎉"
      assert html =~ "Subtract `0xD800`" || html =~ "0000111100 | 1110001001"
    end

    test "lone high surrogate shows the decoder error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-16?bytes=D8%2000&endian=big")
      assert html =~ "doesn&#39;t form a single UTF-16 code point"
    end

    test "the same bytes decode differently per endianness", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-16?bytes=00%20E9&endian=big")
      assert html =~ "U+00E9"

      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-16?bytes=00%20E9&endian=little")
      assert html =~ "U+E900"
    end
  end

  describe "utf-32 encode" do
    test "default example: U+1F389 little endian -> 89 F3 01 00", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-32")

      assert html =~ "U+1F389"
      assert html =~ "0x89 0xF3 0x01 0x00"
      assert html =~ "Convert to 32-bit binary"
      assert html =~ "00000000000000011111001110001001"
    end

    test "big endian writes the natural order", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-32?input=U%2B1F389&endian=big")
      assert html =~ "0x00 0x01 0xF3 0x89"
    end
  end

  describe "utf-32 decode" do
    test "default example: 89 F3 01 00 little endian -> U+1F389", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-32")

      assert html =~ "U+1F389"
      assert html =~ "🎉"
    end

    test "wrong byte count shows the decoder error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-32?bytes=00%2000%20E9")
      assert html =~ "Last byte is incomplete" || html =~ "valid UTF-32"
    end

    test "out-of-range value shows the decoder error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-32?bytes=00%2011%2000%2000&endian=big")
      assert html =~ "valid UTF-32"
    end
  end

  describe "latin1 encode" do
    test "default example: U+00E9 -> 0xE9", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/latin1")

      assert html =~ "U+00E9"
      assert html =~ "0xE9"
      assert html =~ "11101001"
    end

    test "code point beyond U+00FF shows the not-encodable error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/latin1?input=U%2B0100")
      assert html =~ "outside the Latin-1 range"
    end
  end

  describe "latin1 decode" do
    test "default example: E9 -> U+00E9", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/latin1")

      assert html =~ "U+00E9"
      assert html =~ "é"
    end

    test "more than one byte shows the decoder error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/latin1?bytes=C3%20A9")
      assert html =~ "not a valid Latin-1 byte"
    end
  end

  describe "windows-1252 encode" do
    test "default example: U+20AC (Euro) -> 0x80, special range highlighted", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/windows-1252")

      assert html =~ "U+20AC"
      assert html =~ "0x80"
      assert html =~ "Microsoft extension"
    end

    test "identity ranges show their banner", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/windows-1252?input=A")
      assert html =~ "identical to ASCII"

      {:ok, _view, html} = live(conn, "/sandbox/encode/windows-1252?input=%C3%A9")
      assert html =~ "identical to Latin-1"
    end

    test "non-encodable code point shows the table error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/windows-1252?input=U%2B0080")
      assert html =~ "isn&#39;t in the Windows-1252 table"
    end
  end

  describe "windows-1252 decode" do
    test "default example: 0x80 -> U+20AC (the Euro trap)", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/windows-1252")

      assert html =~ "U+20AC"
      assert html =~ "€"
      assert html =~ "Classic trap"
    end

    test "unassigned byte shows the not-decodable error", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/decode/windows-1252?bytes=81")
      assert html =~ "doesn&#39;t map to any character"
    end
  end

  describe "localization" do
    test "every page renders in French under /fr", %{conn: conn} do
      pages = [
        "/fr/sandbox/encode/utf-16",
        "/fr/sandbox/decode/utf-16",
        "/fr/sandbox/encode/utf-32",
        "/fr/sandbox/decode/utf-32",
        "/fr/sandbox/encode/latin1",
        "/fr/sandbox/decode/latin1",
        "/fr/sandbox/encode/windows-1252",
        "/fr/sandbox/decode/windows-1252"
      ]

      for page <- pages do
        {:ok, _view, html} = live(conn, page)
        assert html =~ ~s(lang="fr")
        assert html =~ "Étapes détaillées"
      end
    end

    test "the sandbox nav highlights the active page", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-16")
      assert html =~ "sandbox-nav-link-active"
    end
  end
end
