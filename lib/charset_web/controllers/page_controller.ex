defmodule CharsetWeb.PageController do
  use CharsetWeb, :controller

  def home(conn, _params) do
    render(conn, :home)
  end
end
