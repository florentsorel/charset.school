defmodule Charset.Repo do
  use Ecto.Repo,
    otp_app: :charset,
    adapter: Ecto.Adapters.SQLite3
end
