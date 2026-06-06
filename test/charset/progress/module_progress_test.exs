defmodule Charset.Progress.ModuleProgressTest do
  @moduledoc "Pure domain tests - no DB. Port of the Kotlin ModuleProgress specs."
  use ExUnit.Case, async: true

  alias Charset.Progress.ModuleProgress

  @now ~U[2026-06-05 12:00:00Z]

  defp initial, do: ModuleProgress.initial("token-a", :utf8_encode)

  test "initial progression starts at level 1 with empty counters" do
    progress = initial()

    assert progress.level == 1
    assert progress.streak == 0
    assert progress.attempts == 0
    assert progress.errors == 0
    assert progress.last_played_at == nil
  end

  test "a correct completion bumps attempts and streak" do
    progress = ModuleProgress.record_completion(initial(), true, @now)

    assert progress.attempts == 1
    assert progress.errors == 0
    assert progress.streak == 1
    assert progress.last_played_at == @now
  end

  test "an incorrect completion bumps errors and resets the streak" do
    progress =
      initial()
      |> ModuleProgress.record_completion(true, @now)
      |> ModuleProgress.record_completion(true, @now)
      |> ModuleProgress.record_completion(false, @now)

    assert progress.attempts == 3
    assert progress.errors == 1
    assert progress.streak == 0
    assert progress.level == 1
  end

  test "hitting the streak threshold levels up and resets the streak" do
    progress =
      Enum.reduce(1..5, initial(), fn _n, acc ->
        ModuleProgress.record_completion(acc, true, @now)
      end)

    assert progress.level == 2
    assert progress.streak == 0
    assert progress.attempts == 5
  end

  test "the level is capped at the module's max level" do
    # utf16_encode caps at level 2
    progress = %{ModuleProgress.initial("token-a", :utf16_encode) | level: 2, streak: 4}

    progress = ModuleProgress.record_completion(progress, true, @now)

    assert progress.level == 2
    # The streak keeps counting at max level (no reset without a level-up)
    assert progress.streak == 5
  end
end
