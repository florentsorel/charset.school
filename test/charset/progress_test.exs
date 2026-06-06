defmodule Charset.ProgressTest do
  use App.DataCase, async: false

  alias Charset.Progress

  describe "current_level/2" do
    test "first-time visitors start at level 1" do
      assert Progress.current_level("fresh-token", :utf8_encode) == 1
    end

    test "follows the persisted progression" do
      for _n <- 1..5, do: Progress.record_completion("token-a", :utf8_encode, true)

      assert Progress.current_level("token-a", :utf8_encode) == 2
    end

    test "is clamped to the module's max level" do
      # Force an out-of-range level (legacy data / manual edit) via 20
      # correct completions on a 2-level module: level can never exceed max.
      for _n <- 1..20, do: Progress.record_completion("token-b", :utf16_encode, true)

      assert Progress.current_level("token-b", :utf16_encode) == 2
    end
  end

  describe "record_completion/3" do
    test "creates the row on first completion and accumulates" do
      progress = Progress.record_completion("token-c", :utf8_decode, true)

      assert progress.attempts == 1
      assert progress.streak == 1
      assert progress.errors == 0

      progress = Progress.record_completion("token-c", :utf8_decode, false)

      assert progress.attempts == 2
      assert progress.streak == 0
      assert progress.errors == 1
      assert progress.last_played_at != nil
    end

    test "progressions are isolated per token and module" do
      Progress.record_completion("token-d", :utf8_encode, true)

      assert Progress.find("token-e", :utf8_encode) == nil
      assert Progress.find("token-d", :utf8_decode) == nil
      assert Progress.find("token-d", :utf8_encode).attempts == 1
    end
  end

  describe "find_all/1" do
    test "lists every module progression of the token" do
      Progress.record_completion("token-f", :utf8_encode, true)
      Progress.record_completion("token-f", :latin1_decode, false)

      modules = "token-f" |> Progress.find_all() |> Enum.map(& &1.module) |> Enum.sort()
      assert modules == [:latin1_decode, :utf8_encode]
    end
  end
end
