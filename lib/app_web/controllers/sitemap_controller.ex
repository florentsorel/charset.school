defmodule AppWeb.SitemapController do
  @moduledoc """
  Single sitemap: every page in both locales, each URL carrying its hreflang
  alternates (en-US, fr-FR, x-default). No sitemap index - the site has a
  fixed handful of pages, the old index shape was an artifact of the Nuxt
  module. `/sitemap_index.xml` (the URL search engines learned from the old
  site) permanently redirects here.

  The page list is static (no dynamic content pages); the host comes from
  the endpoint config so staging hosts emit their own URLs.
  """
  use AppWeb, :controller

  # Localizable page paths (EN form; FR is the /fr prefix).
  @paths [
           "/",
           "/sandbox"
         ] ++
           for(
             direction <- ["encode", "decode"],
             encoding <- ["utf-8", "utf-16", "utf-32", "latin1", "windows-1252"],
             do: "/sandbox/#{direction}/#{encoding}"
           ) ++
           for(
             direction <- ["encode", "decode"],
             encoding <- ["utf-8", "utf-16", "utf-32"],
             do: "/exercise/#{direction}/#{encoding}"
           )

  def index(conn, _params) do
    base = base_url(conn)

    entries =
      for path <- @paths, locale <- ["en", "fr"] do
        en = base <> path
        fr = base <> localize_fr(path)
        loc = if locale == "en", do: en, else: fr

        """
          <url>
            <loc>#{loc}</loc>
            <xhtml:link rel="alternate" hreflang="en-US" href="#{en}"/>
            <xhtml:link rel="alternate" hreflang="fr-FR" href="#{fr}"/>
            <xhtml:link rel="alternate" hreflang="x-default" href="#{en}"/>
          </url>\
        """
      end

    xml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
    #{Enum.join(entries, "\n")}
    </urlset>
    """

    conn
    |> put_resp_content_type("application/xml")
    |> send_resp(200, xml)
  end

  # The old Nuxt site exposed a sitemap index; search engines know that URL.
  def legacy_index(conn, _params) do
    conn
    |> put_status(:moved_permanently)
    |> redirect(to: "/sitemap.xml")
  end

  defp localize_fr("/"), do: "/fr"
  defp localize_fr(path), do: "/fr" <> path

  defp base_url(conn) do
    %{scheme: scheme, host: host, port: port} =
      Application.get_env(:app, AppWeb.Endpoint)[:url]
      |> Keyword.take([:scheme, :host, :port])
      |> Enum.into(%{scheme: "https", host: conn.host, port: nil})

    default_port? = port in [nil, 80, 443]
    "#{scheme}://#{host}#{if default_port?, do: "", else: ":#{port}"}"
  end
end
