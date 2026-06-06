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

  test "keeps an existing UUID token without re-minting", %{conn: conn} do
    existing = Ecto.UUID.generate()

    conn =
      conn
      |> put_req_cookie(VisitorToken.cookie_name(), existing)
      |> get(~p"/")

    assert conn.assigns.visitor_token == existing
    refute Map.has_key?(conn.resp_cookies, "token_id")
  end

  test "replaces any non-UUID cookie with a fresh token", %{conn: conn} do
    for crafted <- ["existing-token", "", String.duplicate("a", 65), "1234"] do
      conn =
        conn
        |> put_req_cookie(VisitorToken.cookie_name(), crafted)
        |> get(~p"/")

      assert conn.assigns.visitor_token != crafted
      assert {:ok, _uuid} = Ecto.UUID.cast(conn.assigns.visitor_token)
    end
  end
end
