defmodule App.Repo.Migrations.CreateModuleProgress do
  @moduledoc """
  Per-visitor per-module progression. One row per (token, module_id), the
  token being the opaque anonymous-visitor id from an HttpOnly cookie.
  Updated whenever the visitor fully completes an exercise; the streak
  auto-advances the level.
  """

  use Ecto.Migration

  def change do
    create table(:module_progress) do
      add :token, :string, null: false, size: 64
      add :module_id, :string, null: false, size: 64
      add :level, :integer, null: false, default: 1
      add :streak, :integer, null: false, default: 0
      add :attempts, :integer, null: false, default: 0
      add :errors, :integer, null: false, default: 0
      add :last_played_at, :utc_datetime

      timestamps(type: :utc_datetime)
    end

    create unique_index(:module_progress, [:token, :module_id])
  end
end
