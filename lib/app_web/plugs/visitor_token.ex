defmodule AppWeb.Plugs.VisitorToken do
  @moduledoc """
  Reads - or mints - the anonymous visitor token. Everything the visitor does
  (attempts, progression) is keyed by this opaque UUID; there are no user
  accounts.

  The token lives in a long-lived HttpOnly cookie (the single minting
  authority is this plug - the old stack minted at the Nuxt edge) and is
  copied into the session so LiveViews can read it on the websocket mount,
  where request cookies are not directly available.

  Only tokens we could have minted are accepted: anything that is not a
  canonical UUID (oversized, blank, attacker-crafted) is treated as absent
  and replaced - a malicious cookie can neither overflow the 64-char column
  nor smuggle an arbitrary identifier into the keyspace.
  """

  import Plug.Conn

  @cookie "token_id"
  # ~1 year
  @max_age 60 * 60 * 24 * 365

  def cookie_name, do: @cookie

  def init(opts), do: opts

  def call(conn, _opts) do
    conn = fetch_cookies(conn)

    case valid_token(conn.req_cookies[@cookie]) do
      nil ->
        token = Ecto.UUID.generate()

        conn
        |> put_resp_cookie(@cookie, token,
          http_only: true,
          same_site: "Lax",
          secure: conn.scheme == :https,
          max_age: @max_age
        )
        |> remember(token)

      token ->
        remember(conn, token)
    end
  end

  defp remember(conn, token) do
    conn
    |> assign(:visitor_token, token)
    |> put_session(:visitor_token, token)
  end

  defp valid_token(token) when is_binary(token) do
    case Ecto.UUID.cast(token) do
      {:ok, uuid} -> uuid
      :error -> nil
    end
  end

  defp valid_token(_other), do: nil
end
