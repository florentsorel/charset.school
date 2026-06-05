defmodule Charset.Progress do
  @moduledoc """
  Persistence context for the per-visitor per-module progression.
  """

  import Ecto.Query

  alias App.Repo
  alias Charset.Exercise.ExerciseModule
  alias Charset.Progress.ModuleProgress
  alias Charset.Schema

  @doc """
  Records a completed exercise on the visitor's progression (creating the
  row on first completion) and returns the updated progression.
  """
  @spec record_completion(String.t(), ExerciseModule.t(), boolean()) :: ModuleProgress.t()
  def record_completion(token, module, correct) do
    current = find(token, module) || ModuleProgress.initial(token, module)
    upsert(ModuleProgress.record_completion(current, correct, DateTime.utc_now(:second)))
  end

  @doc """
  The level a new attempt should be generated at. First-time visitors (no
  progression row) start at level 1. Clamped to 1..max_level defensively:
  this is the single source of truth for generation, so a stray out-of-range
  value (legacy data / manual edit) must not make the generator raise.
  """
  @spec current_level(String.t(), ExerciseModule.t()) :: pos_integer()
  def current_level(token, module) do
    level =
      case find(token, module) do
        nil -> 1
        progress -> progress.level
      end

    level |> max(1) |> min(ExerciseModule.max_level(module))
  end

  @spec find(String.t(), ExerciseModule.t()) :: ModuleProgress.t() | nil
  def find(token, module) do
    module_id = ExerciseModule.id(module)

    Schema.ModuleProgress
    |> Repo.get_by(token: token, module_id: module_id)
    |> case do
      nil -> nil
      row -> to_domain(row)
    end
  end

  @spec find_all(String.t()) :: [ModuleProgress.t()]
  def find_all(token) do
    Repo.all(from p in Schema.ModuleProgress, where: p.token == ^token, order_by: p.module_id)
    |> Enum.map(&to_domain/1)
  end

  defp upsert(%ModuleProgress{} = progress) do
    row = %Schema.ModuleProgress{
      token: progress.token,
      module_id: ExerciseModule.id(progress.module),
      level: progress.level,
      streak: progress.streak,
      attempts: progress.attempts,
      errors: progress.errors,
      last_played_at: progress.last_played_at
    }

    Repo.insert!(row,
      on_conflict:
        {:replace, [:level, :streak, :attempts, :errors, :last_played_at, :updated_at]},
      conflict_target: [:token, :module_id]
    )

    progress
  end

  defp to_domain(row) do
    %ModuleProgress{
      token: row.token,
      module: ExerciseModule.from_id(row.module_id),
      level: row.level,
      streak: row.streak,
      attempts: row.attempts,
      errors: row.errors,
      last_played_at: row.last_played_at
    }
  end
end
