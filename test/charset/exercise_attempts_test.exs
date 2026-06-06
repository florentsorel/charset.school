defmodule Charset.ExerciseAttemptsTest do
  use App.DataCase, async: false

  alias Charset.Exercise
  alias Charset.Exercise.AttemptStep
  alias Charset.Exercise.Step
  alias Charset.ExerciseAttempts

  # A synthetic exercise touching every step type, to pin the full
  # Step <-> child-table roundtrip in one pass.
  defp exercise_with_all_step_types do
    Exercise.encode(0x1F389, :utf16be, 2, [
      Step.Format.new!(
        ["format-choice.code-unit.1", "format-choice.code-unit.2"],
        "format-choice.code-unit.2"
      ),
      Step.Binary.new!("00001111001110001001", 20),
      Step.BitGroups.new!(["0000111100", "1110001001"]),
      Step.HexBytes.new!([0xD8, 0x3C, 0xDF, 0x89]),
      Step.CodePointEntry.new!(0x1F389),
      Step.UsefulBitCount.new!(20),
      Step.Offset.new!(0xF389),
      Step.Endianness.new!(:big)
    ])
  end

  describe "create/3 + get/1" do
    test "persists and reloads every step type with its expected values" do
      created = ExerciseAttempts.create("token-a", :utf16_encode, exercise_with_all_step_types())

      assert created.token == "token-a"
      assert created.module == :utf16_encode
      assert created.level == 2
      assert created.code_point == 0x1F389
      assert created.encoding == :utf16be
      refute created.correct
      refute created.finalized
      assert length(created.steps) == 8

      reloaded = ExerciseAttempts.get(created.id)

      assert reloaded.steps |> Enum.map(& &1.step) ==
               exercise_with_all_step_types().steps

      assert Enum.map(reloaded.steps, & &1.position) == Enum.to_list(0..7)
      assert Enum.all?(reloaded.steps, &(&1.attempts == 0 and not &1.correct and not &1.revealed))
      assert Enum.all?(reloaded.steps, &(&1.user_answer == nil))
    end

    test "get/1 returns nil for unknown ids" do
      assert ExerciseAttempts.get(999_999) == nil
    end
  end

  describe "record_step_submission/4" do
    test "bumps the counter, stores outcome and user answer" do
      attempt = ExerciseAttempts.create("token-a", :utf16_encode, exercise_with_all_step_types())
      [format_step | _rest] = attempt.steps

      updated =
        ExerciseAttempts.record_step_submission(
          format_step.id,
          {:format, "format-choice.code-unit.1"},
          false,
          "format.wrong-choice"
        )

      assert updated.attempts == 1
      refute updated.correct
      assert updated.error_type == "format.wrong-choice"
      assert updated.user_answer == {:format, "format-choice.code-unit.1"}

      # A second (correct) submission keeps counting
      updated =
        ExerciseAttempts.record_step_submission(
          format_step.id,
          {:format, "format-choice.code-unit.2"},
          true,
          nil
        )

      assert updated.attempts == 2
      assert updated.correct
      assert updated.error_type == nil
      assert updated.user_answer == {:format, "format-choice.code-unit.2"}
    end

    test "persists answers of every shape" do
      attempt = ExerciseAttempts.create("token-a", :utf16_encode, exercise_with_all_step_types())

      answers = [
        {:format, "format-choice.code-unit.2"},
        {:binary, "00001111001110001001"},
        {:bit_groups, ["0000111100", "1110001001"]},
        {:hex_bytes, [0xD8, 0x3C, 0xDF, 0x89]},
        {:code_point, 0x1F389},
        {:useful_bit_count, 20},
        {:offset, 0xF389},
        {:endianness, :big}
      ]

      for {step, answer} <- Enum.zip(attempt.steps, answers) do
        ExerciseAttempts.record_step_submission(step.id, answer, true, nil)
      end

      reloaded = ExerciseAttempts.get(attempt.id)
      assert Enum.map(reloaded.steps, & &1.user_answer) == answers
    end
  end

  describe "mark_step_revealed/1" do
    test "flips the revealed flag without touching the counter" do
      attempt = ExerciseAttempts.create("token-a", :utf16_encode, exercise_with_all_step_types())
      [step | _rest] = attempt.steps

      revealed = ExerciseAttempts.mark_step_revealed(step.id)

      assert revealed.revealed
      assert revealed.attempts == 0
      assert AttemptStep.resolved?(revealed)
    end
  end

  describe "finalize/3" do
    test "marks the attempt finalized with its outcome" do
      attempt = ExerciseAttempts.create("token-a", :utf16_encode, exercise_with_all_step_types())

      finalized = ExerciseAttempts.finalize(attempt.id, true, 1234)

      assert finalized.finalized
      assert finalized.correct
      assert finalized.duration_ms == 1234
    end
  end

  describe "find_latest_unfinalized/2" do
    test "returns the most recent open attempt of this token and module" do
      _older = ExerciseAttempts.create("token-a", :utf8_encode, simple_exercise())
      newer = ExerciseAttempts.create("token-a", :utf8_encode, simple_exercise())

      found = ExerciseAttempts.find_latest_unfinalized("token-a", :utf8_encode)
      assert found.id == newer.id
    end

    test "skips finalized attempts and other tokens/modules" do
      attempt = ExerciseAttempts.create("token-a", :utf8_encode, simple_exercise())
      ExerciseAttempts.finalize(attempt.id, true, nil)

      assert ExerciseAttempts.find_latest_unfinalized("token-a", :utf8_encode) == nil

      _other = ExerciseAttempts.create("token-b", :utf8_encode, simple_exercise())
      assert ExerciseAttempts.find_latest_unfinalized("token-a", :utf8_encode) == nil

      _other_module = ExerciseAttempts.create("token-a", :utf8_decode, simple_exercise())
      assert ExerciseAttempts.find_latest_unfinalized("token-a", :utf8_encode) == nil
    end
  end

  defp simple_exercise do
    Exercise.encode(0x41, :utf8, 1, [
      Step.Format.new!(["format-choice.byte-count.1"], "format-choice.byte-count.1"),
      Step.HexBytes.new!([0x41])
    ])
  end
end
