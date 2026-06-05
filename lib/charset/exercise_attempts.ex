defmodule Charset.ExerciseAttempts do
  @moduledoc """
  Persistence context for exercise attempts: the parent rows, the per-step
  state and the table-per-type child rows carrying `expected` (the DB is the
  source of truth for validation - defense in depth, the client never sends
  the expected value) and `user_answer`.

  Maps between the Ecto schemas (`Charset.Schema.*`) and the pure domain
  structs (`Charset.Exercise.Attempt` / `AttemptStep`).
  """

  import Ecto.Query

  alias App.Repo
  alias Charset.Encoding
  alias Charset.Exercise
  alias Charset.Exercise.Attempt
  alias Charset.Exercise.AttemptStep
  alias Charset.Exercise.ExerciseModule
  alias Charset.Exercise.Step
  alias Charset.Schema

  @doc """
  Persists a freshly generated exercise as an unfinalized attempt: the parent
  row, one step row per position, and the matching child row with the
  server-side expected values.
  """
  @spec create(String.t(), ExerciseModule.t(), Exercise.t()) :: Attempt.t()
  def create(token, module, %Exercise{} = exercise) do
    {:ok, attempt} =
      Repo.transaction(fn ->
        attempt_row =
          Repo.insert!(%Schema.ExerciseAttempt{
            token: token,
            module_id: ExerciseModule.id(module),
            level: exercise.level,
            code_point: exercise.code_point,
            encoding: Encoding.id(exercise.encoding),
            correct: false,
            finalized: false
          })

        steps =
          exercise.steps
          |> Enum.with_index()
          |> Enum.map(fn {step, position} ->
            step_row =
              Repo.insert!(%Schema.AttemptStep{
                attempt_id: attempt_row.id,
                position: position,
                step_type: Step.type_id(step)
              })

            Repo.insert!(expected_child_row(step_row.id, step))

            %AttemptStep{
              id: step_row.id,
              position: position,
              step: step,
              correct: false,
              error_type: nil,
              attempts: 0,
              revealed: false,
              user_answer: nil
            }
          end)

        to_attempt(attempt_row, steps)
      end)

    attempt
  end

  @spec get(integer()) :: Attempt.t() | nil
  def get(attempt_id) do
    case Repo.get(Schema.ExerciseAttempt, attempt_id) do
      nil -> nil
      attempt_row -> to_attempt(attempt_row, load_steps(attempt_row.id))
    end
  end

  @doc "The most recent unfinalized attempt of this visitor on this module."
  @spec find_latest_unfinalized(String.t(), ExerciseModule.t()) :: Attempt.t() | nil
  def find_latest_unfinalized(token, module) do
    module_id = ExerciseModule.id(module)

    row =
      Repo.one(
        from a in Schema.ExerciseAttempt,
          where: a.token == ^token and a.module_id == ^module_id and a.finalized == false,
          order_by: [desc: a.inserted_at, desc: a.id],
          limit: 1
      )

    case row do
      nil -> nil
      attempt_row -> to_attempt(attempt_row, load_steps(attempt_row.id))
    end
  end

  @doc """
  Records one submission on a step: bumps the attempts counter, stores the
  outcome and the user's answer (in the child row). Returns the updated step.
  """
  @spec record_step_submission(
          integer(),
          Charset.Exercise.Answer.t(),
          boolean(),
          String.t() | nil
        ) ::
          AttemptStep.t()
  def record_step_submission(step_id, user_answer, correct, error_type) do
    {:ok, step} =
      Repo.transaction(fn ->
        step_row = Repo.get!(Schema.AttemptStep, step_id)

        step_row =
          step_row
          |> Ecto.Changeset.change(
            correct: correct,
            error_type: error_type,
            attempts: step_row.attempts + 1
          )
          |> Repo.update!()

        update_user_answer(step_row, user_answer)
        to_attempt_step(step_row)
      end)

    step
  end

  @spec mark_step_revealed(integer()) :: AttemptStep.t()
  def mark_step_revealed(step_id) do
    step_row =
      Schema.AttemptStep
      |> Repo.get!(step_id)
      |> Ecto.Changeset.change(revealed: true)
      |> Repo.update!()

    to_attempt_step(step_row)
  end

  @spec finalize(integer(), boolean(), integer() | nil) :: Attempt.t()
  def finalize(attempt_id, correct, duration_ms) do
    Schema.ExerciseAttempt
    |> Repo.get!(attempt_id)
    |> Ecto.Changeset.change(correct: correct, finalized: true, duration_ms: duration_ms)
    |> Repo.update!()

    get(attempt_id)
  end

  ## Row -> domain assembly

  defp load_steps(attempt_id) do
    rows =
      Repo.all(
        from s in Schema.AttemptStep,
          where: s.attempt_id == ^attempt_id,
          order_by: s.position
      )

    Enum.map(rows, &to_attempt_step/1)
  end

  defp to_attempt(attempt_row, steps) do
    %Attempt{
      id: attempt_row.id,
      token: attempt_row.token,
      module: ExerciseModule.from_id(attempt_row.module_id),
      level: attempt_row.level,
      code_point: attempt_row.code_point,
      encoding: Encoding.from_id(attempt_row.encoding),
      correct: attempt_row.correct,
      finalized: attempt_row.finalized,
      duration_ms: attempt_row.duration_ms,
      steps: steps,
      created_at: attempt_row.inserted_at
    }
  end

  defp to_attempt_step(step_row) do
    {step, user_answer} = load_step_data(step_row.step_type, step_row.id)

    %AttemptStep{
      id: step_row.id,
      position: step_row.position,
      step: step,
      correct: step_row.correct,
      error_type: step_row.error_type,
      attempts: step_row.attempts,
      revealed: step_row.revealed,
      user_answer: user_answer
    }
  end

  ## Step <-> child table mapping

  defp expected_child_row(step_id, %Step.Format{} = step) do
    %Schema.StepFormat{step_id: step_id, choices: step.choices, expected: step.expected}
  end

  defp expected_child_row(step_id, %Step.Binary{} = step) do
    %Schema.StepBinary{step_id: step_id, expected: step.expected, bit_length: step.length}
  end

  defp expected_child_row(step_id, %Step.BitGroups{} = step) do
    %Schema.StepBitGroups{step_id: step_id, expected: step.expected}
  end

  defp expected_child_row(step_id, %Step.HexBytes{} = step) do
    %Schema.StepHexBytes{step_id: step_id, expected: step.expected}
  end

  defp expected_child_row(step_id, %Step.CodePointEntry{} = step) do
    %Schema.StepCodePoint{step_id: step_id, expected: step.expected}
  end

  defp expected_child_row(step_id, %Step.UsefulBitCount{} = step) do
    %Schema.StepUsefulBitCount{step_id: step_id, expected: step.expected}
  end

  defp expected_child_row(step_id, %Step.Offset{} = step) do
    %Schema.StepOffset{step_id: step_id, expected: step.expected}
  end

  defp expected_child_row(step_id, %Step.Endianness{} = step) do
    %Schema.StepEndianness{step_id: step_id, expected: Atom.to_string(step.expected)}
  end

  defp load_step_data("format", step_id) do
    row = Repo.get!(Schema.StepFormat, step_id)
    {Step.Format.new!(row.choices, row.expected), row.user_answer && {:format, row.user_answer}}
  end

  defp load_step_data("binary", step_id) do
    row = Repo.get!(Schema.StepBinary, step_id)

    {Step.Binary.new!(row.expected, row.bit_length),
     row.user_answer && {:binary, row.user_answer}}
  end

  defp load_step_data("bit-groups", step_id) do
    row = Repo.get!(Schema.StepBitGroups, step_id)
    {Step.BitGroups.new!(row.expected), row.user_answer && {:bit_groups, row.user_answer}}
  end

  defp load_step_data("hex-bytes", step_id) do
    row = Repo.get!(Schema.StepHexBytes, step_id)
    {Step.HexBytes.new!(row.expected), row.user_answer && {:hex_bytes, row.user_answer}}
  end

  defp load_step_data("code-point", step_id) do
    row = Repo.get!(Schema.StepCodePoint, step_id)
    {Step.CodePointEntry.new!(row.expected), row.user_answer && {:code_point, row.user_answer}}
  end

  defp load_step_data("useful-bit-count", step_id) do
    row = Repo.get!(Schema.StepUsefulBitCount, step_id)

    {Step.UsefulBitCount.new!(row.expected),
     row.user_answer && {:useful_bit_count, row.user_answer}}
  end

  defp load_step_data("offset", step_id) do
    row = Repo.get!(Schema.StepOffset, step_id)
    {Step.Offset.new!(row.expected), row.user_answer && {:offset, row.user_answer}}
  end

  defp load_step_data("endianness", step_id) do
    row = Repo.get!(Schema.StepEndianness, step_id)
    expected = String.to_existing_atom(row.expected)

    {Step.Endianness.new!(expected),
     row.user_answer && {:endianness, String.to_existing_atom(row.user_answer)}}
  end

  # A mismatched answer kind (caught as answer.type-mismatch by the
  # validator) has no row in its child table - the submission is counted on
  # the parent but the answer itself is dropped, like the old UPDATE-no-row
  # behavior on main.
  defp update_user_answer(step_row, answer) do
    {schema, value} = answer_child(answer)

    case Repo.get(schema, step_row.id) do
      nil ->
        :ok

      child_row ->
        child_row
        |> Ecto.Changeset.change(user_answer: value)
        |> Repo.update!()
    end
  end

  defp answer_child({:format, value}), do: {Schema.StepFormat, value}
  defp answer_child({:binary, bits}), do: {Schema.StepBinary, bits}
  defp answer_child({:bit_groups, groups}), do: {Schema.StepBitGroups, groups}
  defp answer_child({:hex_bytes, bytes}), do: {Schema.StepHexBytes, bytes}
  defp answer_child({:code_point, value}), do: {Schema.StepCodePoint, value}
  defp answer_child({:useful_bit_count, value}), do: {Schema.StepUsefulBitCount, value}
  defp answer_child({:offset, value}), do: {Schema.StepOffset, value}
  defp answer_child({:endianness, value}), do: {Schema.StepEndianness, Atom.to_string(value)}
end
