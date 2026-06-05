defmodule AppWeb.Plugs.VisitorTokenTest do
  use AppWeb.ConnCase, async: true

  alias AppWeb.Plugs.VisitorToken

  test "mints a token cookie on first visit and copies it to the session", %{conn: conn} do
    conn = get(conn, ~p"/")

    %{value: token, http_only: true, same_site: "Lax"} = conn.resp_cookies["token_id"]
    assert {:ok, _uuid} = Ecto.UUID.cast(token)
    assert conn.assigns.visitor_token == token
    assert get_session(conn, :visitor_token) == token
  end

  test "keeps an existing valid token without re-minting", %{conn: conn} do
    conn =
      conn
      |> put_req_cookie(VisitorToken.cookie_name(), "existing-token")
      |> get(~p"/")

    assert conn.assigns.visitor_token == "existing-token"
    refute Map.has_key?(conn.resp_cookies, "token_id")
  end

  test "replaces a blank or oversized cookie with a fresh token", %{conn: conn} do
    oversized = String.duplicate("a", 65)

    conn =
      conn
      |> put_req_cookie(VisitorToken.cookie_name(), oversized)
      |> get(~p"/")

    assert conn.assigns.visitor_token != oversized
    assert {:ok, _uuid} = Ecto.UUID.cast(conn.assigns.visitor_token)
  end
end
