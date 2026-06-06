defmodule AppWeb.SitemapControllerTest do
  use AppWeb.ConnCase, async: true

  test "lists every page in both locales with hreflang alternates", %{conn: conn} do
    conn = get(conn, ~p"/sitemap.xml")
    body = response(conn, 200)

    assert response_content_type(conn, :xml) =~ "application/xml"

    # (2 base pages + 10 sandbox + 6 exercise) x 2 locales
    assert length(String.split(body, "<url>")) - 1 == 36
    assert body =~ "<loc>https://localhost/sandbox/encode/utf-8</loc>"
    assert body =~ "<loc>https://localhost/fr/sandbox/encode/utf-8</loc>"
    assert body =~ "<loc>https://localhost/exercise/decode/utf-32</loc>"
    assert body =~ "<loc>https://localhost/fr</loc>"

    assert body =~
             ~s(<xhtml:link rel="alternate" hreflang="fr-FR" href="https://localhost/fr/sandbox/encode/utf-8"/>)

    assert body =~
             ~s(<xhtml:link rel="alternate" hreflang="x-default" href="https://localhost/"/>)
  end

  test "the old sitemap index URL permanently redirects", %{conn: conn} do
    conn = get(conn, ~p"/sitemap_index.xml")

    assert redirected_to(conn, 301) == "/sitemap.xml"
  end
end
