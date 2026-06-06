defmodule App.Repo.Migrations.CreateExerciseTrackingTables do
  @moduledoc """
  Exercise tracking: relational, table-per-StepType (ported from main's
  Postgres schema; the array columns become {:array, _} fields stored as
  JSON text by ecto_sqlite3).

  Parent `exercise_attempts` aggregates one full attempt (one exercise) by
  one anonymous visitor (identified by an opaque `token` from an HttpOnly
  cookie). Parent `attempt_steps` aggregates the micro-questions inside an
  attempt, with a `step_type` discriminator. Eight child tables (one per
  step type) hold the type-specific data - `expected` (server-side, never
  leaves the DB unrevealed) and `user_answer` (filled progressively as the
  visitor submits step by step).

  No CHECK constraints on enumerated values: the app is the only writer and
  the Elixir atoms are the source of truth. FKs stay (referential integrity).
  """

  use Ecto.Migration

  def change do
    create table(:exercise_attempts) do
      add :token, :string, null: false, size: 64
      add :module_id, :string, null: false, size: 64
      add :level, :integer, null: false
      add :code_point, :integer, null: false
      add :encoding, :string, null: false, size: 16
      add :correct, :boolean, null: false, default: false
      add :finalized, :boolean, null: false, default: false
      add :duration_ms, :integer

      timestamps(updated_at: false, type: :utc_datetime)
    end

    create index(:exercise_attempts, [:token, :module_id])

    # Parent step table with `step_type` discriminator. `attempts` counts how
    # many times the user has submitted this step (used to escalate the hint
    # level on the next wrong submit, and to gate the "give me the answer"
    # button at the reveal threshold).
    create table(:attempt_steps) do
      add :attempt_id, references(:exercise_attempts, on_delete: :delete_all), null: false
      add :position, :integer, null: false
      add :step_type, :string, null: false, size: 32
      add :correct, :boolean, null: false, default: false
      add :error_type, :string, size: 64
      add :attempts, :integer, null: false, default: 0
      add :revealed, :boolean, null: false, default: false
    end

    create unique_index(:attempt_steps, [:attempt_id, :position])

    # Child tables: one per step type. PK = step_id (FK to attempt_steps.id).

    create table(:attempt_step_format, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :choices, {:array, :string}, null: false
      add :expected, :string, null: false, size: 64
      add :user_answer, :string, size: 64
    end

    create table(:attempt_step_binary, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, :string, null: false, size: 64
      add :bit_length, :integer, null: false
      add :user_answer, :string, size: 64
    end

    create table(:attempt_step_bit_groups, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, {:array, :string}, null: false
      add :user_answer, {:array, :string}
    end

    create table(:attempt_step_hex_bytes, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, {:array, :integer}, null: false
      add :user_answer, {:array, :integer}
    end

    create table(:attempt_step_code_point, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, :integer, null: false
      add :user_answer, :integer
    end

    create table(:attempt_step_useful_bit_count, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, :integer, null: false
      add :user_answer, :integer
    end

    create table(:attempt_step_offset, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, :integer, null: false
      add :user_answer, :integer
    end

    create table(:attempt_step_endianness, primary_key: false) do
      add :step_id, references(:attempt_steps, on_delete: :delete_all), primary_key: true
      add :expected, :string, null: false, size: 16
      add :user_answer, :string, size: 16
    end
  end
end
