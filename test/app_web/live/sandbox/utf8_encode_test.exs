defmodule AppWeb.SandboxLive.Utf8EncodeTest do
  use AppWeb.ConnCase, async: true

  import Phoenix.LiveViewTest

  test "renders the default example (U+00E9) with its full breakdown", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/encode/utf-8")

    assert html =~ "U+00E9"
    assert html =~ "é"
    # hex bytes of é
    assert html =~ "0xC3 0xA9"
    # bit groups step
    assert html =~ "00011 | 101001"
    # format choice label
    assert html =~ "2 bytes · U+0080 → U+07FF"
  end

  test "input change patches the URL and recomputes the breakdown", %{conn: conn} do
    {:ok, view, _html} = live(conn, "/sandbox/encode/utf-8")

    html =
      view
      |> element("form")
      |> render_change(%{"input" => "U+1F389"})

    assert_patch(view)
    assert html =~ "U+1F389"
    assert html =~ "🎉"
    assert html =~ "0xF0 0x9F 0x8E 0x89"
    assert html =~ "4 bytes · U+10000 → U+10FFFF"
  end

  test "supports all accepted input shapes via the query param", %{conn: conn} do
    for input <- ["0xE9", "233", "é"] do
      {:ok, _view, html} = live(conn, "/sandbox/encode/utf-8?input=#{URI.encode_www_form(input)}")
      assert html =~ "U+00E9"
    end
  end

  test "repairs the +-decoded-as-space URL shape (?input=U 1F389)", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/encode/utf-8?input=U%201F389")
    assert html =~ "U+1F389"
  end

  test "ASCII input collapses to the 2-step flow (no binary, no split)", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/encode/utf-8?input=A")

    assert html =~ "U+0041"
    assert html =~ "1 byte · U+0000 → U+007F"
    refute html =~ "Split into chunks"
  end

  test "invisible code points show the mnemonic label instead of a glyph", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/encode/utf-8?input=U%2B0007")

    assert html =~ "BEL"
    assert html =~ "Bell"
  end

  test "invalid input shows the error and hides the breakdown", %{conn: conn} do
    {:ok, view, html} = live(conn, "/sandbox/encode/utf-8?input=zz")

    assert html =~ "Unrecognised format."
    refute html =~ "Step-by-step breakdown"

    # surrogate rejection
    html = view |> element("form") |> render_change(%{"input" => "U+D800"})
    assert html =~ "Surrogates (U+D800 → U+DFFF) are not valid UTF-8."
  end

  test "renders in French under /fr", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/fr/sandbox/encode/utf-8")

    assert html =~ "Bac à sable UTF-8 (encodage)" or html =~ "2 octets · U+0080 → U+07FF"
  end

  test "/sandbox redirects to the utf-8 encode page in both locales", %{conn: conn} do
    assert redirected_to(get(conn, "/sandbox")) == "/sandbox/encode/utf-8"
    assert redirected_to(get(conn, "/fr/sandbox")) == "/fr/sandbox/encode/utf-8"
  end
end
