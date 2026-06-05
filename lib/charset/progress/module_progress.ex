defmodule Charset.Progress.ModuleProgress do
  @moduledoc """
  Per-visitor per-module progression. Pure domain struct - the Ecto mapping
  lives in `Charset.Progress`.
  """

  alias Charset.Exercise.ExerciseModule

  @streak_for_level_up 5

  @enforce_keys [:token, :module, :level, :streak, :attempts, :errors]
  defstruct [:token, :module, :level, :streak, :attempts, :errors, :last_played_at]

  @type t :: %__MODULE__{
          token: String.t(),
          module: ExerciseModule.t(),
          level: pos_integer(),
          streak: non_neg_integer(),
          attempts: non_neg_integer(),
          errors: non_neg_integer(),
          last_played_at: DateTime.t() | nil
        }

  def streak_for_level_up, do: @streak_for_level_up

  @spec initial(String.t(), ExerciseModule.t()) :: t()
  def initial(token, module) do
    %__MODULE__{token: token, module: module, level: 1, streak: 0, attempts: 0, errors: 0}
  end

  @doc """
  Records a completed exercise. Auto-advances the level once the streak
  threshold is hit (while still below the module's max level), then resets
  the streak so the user has to earn the next bump as well. The level is not
  user-selectable - the backend drives progression entirely.
  """
  @spec record_completion(t(), boolean(), DateTime.t()) :: t()
  def record_completion(%__MODULE__{} = progress, correct, now) do
    updated = %{
      progress
      | attempts: progress.attempts + 1,
        errors: if(correct, do: progress.errors, else: progress.errors + 1),
        streak: if(correct, do: progress.streak + 1, else: 0),
        last_played_at: now
    }

    if updated.streak >= @streak_for_level_up and
         updated.level < ExerciseModule.max_level(progress.module) do
      %{updated | level: updated.level + 1, streak: 0}
    else
      updated
    end
  end
end
