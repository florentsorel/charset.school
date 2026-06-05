defmodule CharsetWeb.PageController do
  use CharsetWeb, :controller

  def home(conn, _params) do
    render(conn, :home, page_title: gettext("Learn character encoding"))
  end
end
