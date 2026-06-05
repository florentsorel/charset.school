defmodule Charset.Schema.ModuleProgress do
  @moduledoc "Ecto schema for the `module_progress` table."

  use Ecto.Schema

  schema "module_progress" do
    field :token, :string
    field :module_id, :string
    field :level, :integer, default: 1
    field :streak, :integer, default: 0
    field :attempts, :integer, default: 0
    field :errors, :integer, default: 0
    field :last_played_at, :utc_datetime

    timestamps(type: :utc_datetime)
  end
end
