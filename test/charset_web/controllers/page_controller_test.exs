defmodule CharsetWeb.PageControllerTest do
  use CharsetWeb.ConnCase

  test "GET / renders the landing in English", %{conn: conn} do
    response = conn |> get(~p"/") |> html_response(200)

    assert response =~ ~s(<html lang="en")
    assert response =~ "Learn character encoding,"
    assert response =~ "one bit at a time."
    # locale toggle points to the same page in French
    assert response =~ ~s(href="/fr")
  end

  test "GET /fr renders the landing in French", %{conn: conn} do
    response = conn |> get("/fr") |> html_response(200)

    assert response =~ ~s(<html lang="fr")
    assert response =~ "un bit à la fois."
    # locale toggle points back to the English page
    assert response =~ ~s(href="/")
  end
end
