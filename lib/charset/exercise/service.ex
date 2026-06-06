defmodule Charset.Exercise.Service do
  @moduledoc """
  Orchestrates the exercise lifecycle (port of the Kotlin `ExerciseService`):

    * `generate/2` - level driven by the visitor's persisted progression
      (auto-advanced by streak), random byte order for UTF-16/32 modules,
      attempt persisted with its server-side expected values
    * `validate_step/4` - validates against the DB-loaded step (defense in
      depth: the client never supplies the expected value), records the
      submission, finalizes the attempt once every step is resolved
    * `reveal_step/3` - "give me the answer", gated behind the reveal
      threshold; a revealed step makes the whole attempt incorrect
    * sole-ownership guard: an attempt is scoped to the visitor token that
      created it (an unguessable UUID from an HttpOnly cookie) - foreign
      attempts are reported as not found

  Errors are tagged atoms rather than exceptions: stale or tampered clients
  (double submits, replayed ids) are expected inputs for a web endpoint.
  """

  alias Charset.Exercise.Answer
  alias Charset.Exercise.AnswerValidator
  alias Charset.Exercise.Attempt
  alias Charset.Exercise.AttemptStep
  alias Charset.Exercise.ExerciseModule
  alias Charset.Exercise.Generator.ExerciseGenerator
  alias Charset.Exercise.Step
  alias Charset.Exercise.ValidationResult
  alias Charset.ExerciseAttempts
  alias Charset.Progress

  @reveal_threshold 3

  @type error ::
          :attempt_not_found
          | :already_finalized
          | :step_not_found
          | :step_already_resolved
          | :reveal_not_allowed

  def reveal_threshold, do: @reveal_threshold

  @spec generate(String.t(), ExerciseModule.t()) :: Attempt.t()
  def generate(token, module) do
    level = Progress.current_level(token, module)
    encoding = pick_encoding(module)

    exercise =
      case ExerciseModule.direction(module) do
        :encode -> ExerciseGenerator.generate_encode!(encoding, level)
        :decode -> ExerciseGenerator.generate_decode!(encoding, level)
      end

    ExerciseAttempts.create(token, module, exercise)
  end

  @spec find_resumable(String.t(), ExerciseModule.t()) :: Attempt.t() | nil
  def find_resumable(token, module), do: ExerciseAttempts.find_latest_unfinalized(token, module)

  @spec validate_step(String.t(), integer(), non_neg_integer(), Answer.t()) ::
          {:ok,
           %{
             validation: ValidationResult.t(),
             step: AttemptStep.t(),
             attempt: Attempt.t(),
             finalized: boolean()
           }}
          | {:error, error()}
  def validate_step(token, attempt_id, step_index, answer) do
    with {:ok, attempt} <- load_owned_attempt(token, attempt_id),
         {:ok, target_step} <- fetch_pending_step(attempt, step_index) do
      result = AnswerValidator.validate(target_step.step, answer)

      updated_step =
        ExerciseAttempts.record_step_submission(
          target_step.id,
          answer,
          result.ok,
          result.error_type
        )

      refreshed = ExerciseAttempts.get(attempt_id)
      finalized = maybe_finalize(refreshed)

      {:ok,
       %{
         validation: result,
         step: updated_step,
         attempt: finalized || refreshed,
         finalized: finalized != nil
       }}
    end
  end

  @spec reveal_step(String.t(), integer(), non_neg_integer()) ::
          {:ok, %{step: AttemptStep.t(), attempt: Attempt.t(), expected: Step.t()}}
          | {:error, error()}
  def reveal_step(token, attempt_id, step_index) do
    with {:ok, attempt} <- load_owned_attempt(token, attempt_id),
         {:ok, target_step} <- fetch_pending_step(attempt, step_index),
         :ok <- check_reveal_allowed(target_step) do
      revealed_step = ExerciseAttempts.mark_step_revealed(target_step.id)
      refreshed = ExerciseAttempts.get(attempt_id)
      finalized = maybe_finalize(refreshed)

      {:ok, %{step: revealed_step, attempt: finalized || refreshed, expected: target_step.step}}
    end
  end

  defp load_owned_attempt(token, attempt_id) do
    case ExerciseAttempts.get(attempt_id) do
      nil ->
        {:error, :attempt_not_found}

      # Sole ownership: a foreign attempt is indistinguishable from a missing one.
      %Attempt{token: other} when other != token ->
        {:error, :attempt_not_found}

      %Attempt{finalized: true} ->
        {:error, :already_finalized}

      attempt ->
        {:ok, attempt}
    end
  end

  defp fetch_pending_step(attempt, step_index) do
    case Enum.at(attempt.steps, step_index) do
      nil ->
        {:error, :step_not_found}

      step ->
        if AttemptStep.resolved?(step), do: {:error, :step_already_resolved}, else: {:ok, step}
    end
  end

  defp check_reveal_allowed(step) do
    if step.attempts >= @reveal_threshold do
      :ok
    else
      {:error, :reveal_not_allowed}
    end
  end

  # Finalizes once every step is resolved (correct or revealed). The attempt
  # only counts as correct when every step was answered correctly without a
  # single reveal; completion is recorded on the progression either way.
  defp maybe_finalize(attempt) do
    if not attempt.finalized and Enum.all?(attempt.steps, &AttemptStep.resolved?/1) do
      correct =
        Enum.all?(attempt.steps, & &1.correct) and not Enum.any?(attempt.steps, & &1.revealed)

      result = ExerciseAttempts.finalize(attempt.id, correct, nil)
      Progress.record_completion(attempt.token, attempt.module, correct)
      result
    end
  end

  # UTF-16/32 modules draw a random byte order per attempt; the exercise
  # header tells the learner which one to target.
  defp pick_encoding(module) do
    case module do
      m when m in [:utf8_encode, :utf8_decode] -> :utf8
      m when m in [:latin1_encode, :latin1_decode] -> :latin1
      m when m in [:windows1252_encode, :windows1252_decode] -> :windows1252
      m when m in [:utf16_encode, :utf16_decode] -> Enum.random([:utf16be, :utf16le])
      m when m in [:utf32_encode, :utf32_decode] -> Enum.random([:utf32be, :utf32le])
    end
  end
end
