defmodule Charset.Schema.ExerciseAttempt do
  @moduledoc "Ecto schema for the `exercise_attempts` parent table."

  use Ecto.Schema

  schema "exercise_attempts" do
    field :token, :string
    field :module_id, :string
    field :level, :integer
    field :code_point, :integer
    field :encoding, :string
    field :correct, :boolean, default: false
    field :finalized, :boolean, default: false
    field :duration_ms, :integer

    has_many :steps, Charset.Schema.AttemptStep, foreign_key: :attempt_id

    timestamps(updated_at: false, type: :utc_datetime)
  end
end
