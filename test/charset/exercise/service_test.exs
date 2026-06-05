defmodule Charset.Exercise.ServiceTest do
  use App.DataCase, async: false

  alias Charset.Exercise.AttemptStep
  alias Charset.Exercise.Service
  alias Charset.Exercise.Step
  alias Charset.ExerciseAttempts
  alias Charset.Progress

  @token "11111111-1111-1111-1111-111111111111"

  describe "generate/2" do
    test "persists an attempt at the visitor's current level" do
      attempt = Service.generate(@token, :utf8_encode)

      assert attempt.token == @token
      assert attempt.module == :utf8_encode
      assert attempt.level == 1
      refute attempt.finalized
      assert attempt.steps != []
      assert ExerciseAttempts.get(attempt.id).id == attempt.id
    end

    test "level follows the persisted progression" do
      for _n <- 1..5, do: Progress.record_completion(@token, :utf8_encode, true)

      attempt = Service.generate(@token, :utf8_encode)
      assert attempt.level == 2
    end

    test "utf-16 modules draw one of the two byte orders" do
      encodings =
        for _n <- 1..30, uniq: true do
          Service.generate(@token, :utf16_decode).encoding
        end

      assert Enum.sort(encodings) == [:utf16be, :utf16le]
    end
  end

  describe "find_resumable/2" do
    test "returns the open attempt, nil once finalized" do
      attempt = Service.generate(@token, :utf8_encode)
      assert Service.find_resumable(@token, :utf8_encode).id == attempt.id

      resolve_all_steps(attempt)
      assert Service.find_resumable(@token, :utf8_encode) == nil
    end
  end

  describe "validate_step/4" do
    test "a correct answer resolves the step" do
      attempt = Service.generate(@token, :utf8_encode)
      [first | _rest] = attempt.steps

      {:ok, outcome} =
        Service.validate_step(@token, attempt.id, 0, correct_answer_for(first.step))

      assert outcome.validation.ok
      assert outcome.step.correct
      assert outcome.step.attempts == 1
    end

    test "a wrong answer records the error and counts the submission" do
      attempt = Service.generate(@token, :utf8_encode)

      {:ok, outcome} = Service.validate_step(@token, attempt.id, 0, {:binary, "0"})

      refute outcome.validation.ok
      refute outcome.step.correct
      assert outcome.step.attempts == 1
      assert outcome.step.error_type != nil
      refute outcome.finalized
    end

    test "resolving every step finalizes the attempt and records the progression" do
      attempt = Service.generate(@token, :utf8_encode)

      outcomes =
        for {step, index} <- Enum.with_index(attempt.steps) do
          {:ok, outcome} =
            Service.validate_step(@token, attempt.id, index, correct_answer_for(step.step))

          outcome
        end

      final = List.last(outcomes)
      assert final.finalized
      assert final.attempt.finalized
      assert final.attempt.correct

      progress = Progress.find(@token, :utf8_encode)
      assert progress.attempts == 1
      assert progress.streak == 1
    end

    test "guards: foreign token, finalized attempt, bad index, resolved step" do
      attempt = Service.generate(@token, :utf8_encode)
      [first | _rest] = attempt.steps

      assert Service.validate_step("other-token", attempt.id, 0, {:binary, "0"}) ==
               {:error, :attempt_not_found}

      assert Service.validate_step(@token, 999_999, 0, {:binary, "0"}) ==
               {:error, :attempt_not_found}

      assert Service.validate_step(@token, attempt.id, 99, {:binary, "0"}) ==
               {:error, :step_not_found}

      {:ok, _outcome} =
        Service.validate_step(@token, attempt.id, 0, correct_answer_for(first.step))

      assert Service.validate_step(@token, attempt.id, 0, correct_answer_for(first.step)) ==
               {:error, :step_already_resolved}

      resolve_all_steps(ExerciseAttempts.get(attempt.id))

      assert Service.validate_step(@token, attempt.id, 0, {:binary, "0"}) ==
               {:error, :already_finalized}
    end
  end

  describe "reveal_step/3" do
    test "is gated behind the reveal threshold" do
      attempt = Service.generate(@token, :utf8_encode)

      assert Service.reveal_step(@token, attempt.id, 0) == {:error, :reveal_not_allowed}

      for _n <- 1..Service.reveal_threshold() do
        {:ok, _outcome} =
          Service.validate_step(@token, attempt.id, 0, wrong_answer_for(hd(attempt.steps).step))
      end

      {:ok, outcome} = Service.reveal_step(@token, attempt.id, 0)

      assert outcome.step.revealed
      assert outcome.expected == hd(attempt.steps).step
    end

    test "a revealed step makes the finalized attempt incorrect" do
      attempt = Service.generate(@token, :utf8_encode)
      [first | rest] = attempt.steps

      for _n <- 1..Service.reveal_threshold() do
        {:ok, _outcome} =
          Service.validate_step(@token, attempt.id, 0, wrong_answer_for(first.step))
      end

      {:ok, _outcome} = Service.reveal_step(@token, attempt.id, 0)

      final =
        for {step, index} <- Enum.with_index(rest, 1), reduce: nil do
          _acc ->
            {:ok, outcome} =
              Service.validate_step(@token, attempt.id, index, correct_answer_for(step.step))

            outcome
        end

      assert final.finalized
      refute final.attempt.correct

      # The failed completion resets the streak
      assert Progress.find(@token, :utf8_encode).streak == 0
    end
  end

  ## Helpers

  defp resolve_all_steps(attempt) do
    for {step, index} <- Enum.with_index(attempt.steps), not AttemptStep.resolved?(step) do
      {:ok, _outcome} =
        Service.validate_step(attempt.token, attempt.id, index, correct_answer_for(step.step))
    end
  end

  defp correct_answer_for(%Step.Format{expected: expected}), do: {:format, expected}
  defp correct_answer_for(%Step.Binary{expected: expected}), do: {:binary, expected}
  defp correct_answer_for(%Step.BitGroups{expected: expected}), do: {:bit_groups, expected}
  defp correct_answer_for(%Step.HexBytes{expected: expected}), do: {:hex_bytes, expected}
  defp correct_answer_for(%Step.CodePointEntry{expected: expected}), do: {:code_point, expected}

  defp correct_answer_for(%Step.UsefulBitCount{expected: expected}),
    do: {:useful_bit_count, expected}

  defp correct_answer_for(%Step.Offset{expected: expected}), do: {:offset, expected}
  defp correct_answer_for(%Step.Endianness{expected: expected}), do: {:endianness, expected}

  defp wrong_answer_for(%Step.Format{choices: choices, expected: expected}),
    do: {:format, Enum.find(choices, &(&1 != expected)) || "format-choice.byte-count.4"}

  defp wrong_answer_for(%Step.Binary{expected: expected, length: length}),
    do: {:binary, flip_bits(expected, length)}

  defp wrong_answer_for(%Step.HexBytes{expected: expected}),
    do: {:hex_bytes, Enum.map(expected, &rem(&1 + 1, 256))}

  defp wrong_answer_for(_step), do: {:code_point, 0x40}

  defp flip_bits(bits, _length) do
    String.replace(bits, ~r/./, fn
      "0" -> "1"
      "1" -> "0"
    end)
  end
end
