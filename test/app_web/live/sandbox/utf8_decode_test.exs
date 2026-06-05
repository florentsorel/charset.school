defmodule AppWeb.SandboxLive.Utf8DecodeTest do
  use AppWeb.ConnCase, async: true

  import Phoenix.LiveViewTest

  test "renders the default example (C3 A9 -> U+00E9) with its full breakdown", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/decode/utf-8")

    assert html =~ "U+00E9"
    assert html =~ "é"
    assert html =~ "0xC3 0xA9"
    assert html =~ "00011 | 101001"
    assert html =~ "2 bytes · U+0080 → U+07FF"
  end

  test "input change patches the URL and recomputes the breakdown", %{conn: conn} do
    {:ok, view, _html} = live(conn, "/sandbox/decode/utf-8")

    html =
      view
      |> element("form")
      |> render_change(%{"input" => "F0 9F 8E 89"})

    assert_patch(view)
    assert html =~ "U+1F389"
    assert html =~ "🎉"
  end

  test "accepts generous byte input shapes", %{conn: conn} do
    for input <- ["C3A9", "0xC3 0xA9", "c3,a9"] do
      {:ok, _view, html} = live(conn, "/sandbox/decode/utf-8?bytes=#{URI.encode_www_form(input)}")
      assert html =~ "U+00E9"
    end
  end

  test "single-byte ASCII input collapses to the short flow", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/decode/utf-8?bytes=41")

    assert html =~ "U+0041"
    refute html =~ "Extract the data bits"
  end

  test "invalid hex shows the parser error", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/sandbox/decode/utf-8?bytes=ZZ")
    assert html =~ "Non-hex characters detected."
  end

  test "structurally invalid UTF-8 shows the decoder error", %{conn: conn} do
    # C0 80 = overlong encoding, rejected by the codec
    {:ok, _view, html} = live(conn, "/sandbox/decode/utf-8?bytes=C0%2080")
    assert html =~ "This byte sequence is not valid UTF-8."

    # lone continuation byte
    {:ok, _view, html} = live(conn, "/sandbox/decode/utf-8?bytes=80")
    assert html =~ "This byte sequence is not valid UTF-8."
  end

  test "renders in French under /fr", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/fr/sandbox/decode/utf-8?bytes=C3+A9")
    assert html =~ "2 octets · U+0080 → U+07FF"
  end
end
