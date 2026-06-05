defmodule AppWeb.SandboxController do
  use AppWeb, :controller

  # /sandbox is an index-less section: land on the first module, like the
  # old frontend's sandbox/index.vue redirect.
  def index(conn, _params) do
    redirect(conn, to: localized_path(conn.assigns.locale, "/sandbox/encode/utf-8"))
  end
end
