defmodule AppWeb.ExerciseLiveTest do
  use AppWeb.ConnCase, async: false

  import Phoenix.LiveViewTest

  alias App.Repo
  alias Charset.Exercise.Step
  alias Charset.Schema

  # Mint the visitor token once: live/2 never rebinds conn, so without a
  # prior dispatched request each call would start cookie-less and get a
  # fresh token (orphaning the attempt the previous call created).
  setup %{conn: conn} do
    %{conn: get(conn, ~p"/")}
  end

  defp latest_attempt do
    import Ecto.Query

    Repo.one(from a in Schema.ExerciseAttempt, order_by: [desc: a.id], limit: 1)
    |> then(&Charset.ExerciseAttempts.get(&1.id))
  end

  defp submit_step(view, params) do
    view |> element("form.exercise-step-input") |> render_submit(params)
  end

  defp correct_params(%Step.Format{expected: expected}), do: %{"value" => expected}

  defp correct_params(%Step.Binary{expected: expected}),
    do: %{"bits" => String.graphemes(expected)}

  defp correct_params(%Step.BitGroups{expected: expected}) do
    groups =
      expected
      |> Enum.with_index()
      |> Map.new(fn {group, index} -> {Integer.to_string(index), String.graphemes(group)} end)

    %{"groups" => groups}
  end

  defp correct_params(%Step.HexBytes{expected: expected}) do
    %{"bytes" => Enum.map(expected, &(&1 |> Integer.to_string(16) |> String.pad_leading(2, "0")))}
  end

  defp correct_params(%Step.CodePointEntry{expected: expected}),
    do: %{"value" => Integer.to_string(expected, 16)}

  defp correct_params(%Step.UsefulBitCount{expected: expected}),
    do: %{"value" => Integer.to_string(expected)}

  defp correct_params(%Step.Offset{expected: expected}),
    do: %{"value" => Integer.to_string(expected, 16)}

  defp wrong_params(%Step.Format{choices: choices, expected: expected}),
    do: %{"value" => Enum.find(choices, &(&1 != expected))}

  defp wrong_params(%Step.Binary{expected: expected}) do
    flipped =
      String.replace(expected, ~r/./, fn
        "0" -> "1"
        "1" -> "0"
      end)

    %{"bits" => String.graphemes(flipped)}
  end

  defp wrong_params(_step), do: %{"value" => "40"}

  defp solve_through(view, attempt, range) do
    for index <- range do
      step = Enum.at(attempt.steps, index)
      submit_step(view, correct_params(step.step))
    end
    |> List.last()
  end

  describe "mount" do
    test "renders the prompt, the progression pill and the first active step", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/exercise/encode/utf-8")

      assert html =~ "Encode this code point in UTF-8."
      assert html =~ "Level 1 ·"
      assert html =~ "step-dot-active"
      # The generated code point shows as the prompt
      attempt = latest_attempt()
      assert html =~ "U+" <> String.pad_leading(Integer.to_string(attempt.code_point, 16), 4, "0")
    end

    test "never leaks any expected value into the HTML (anti-cheat)", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/exercise/decode/utf-16")
      attempt = latest_attempt()

      # Scan the rendered view, minus the signed LiveView blobs (base64 can
      # coincidentally contain "U+XXXX"-shaped substrings).
      html = Regex.replace(~r/data-phx-[a-z-]+="[^"]*"/, render(view), "")

      for step <- attempt.steps do
        case step.step do
          %Step.Binary{expected: expected} ->
            refute html =~ expected

          %Step.BitGroups{expected: expected} ->
            Enum.each(expected, &refute(html =~ &1))

          %Step.CodePointEntry{expected: expected} ->
            refute html =~ "U+" <> String.pad_leading(Integer.to_string(expected, 16), 4, "0")

          _public_or_prompt ->
            :ok
        end
      end
    end

    test "the dead render does not create attempts", %{conn: conn} do
      conn = get(conn, "/exercise/encode/utf-8")
      assert html_response(conn, 200)
      assert Repo.aggregate(Schema.ExerciseAttempt, :count) == 0
    end
  end

  describe "solving" do
    test "a full correct run finalizes with the success message and updates the streak", %{
      conn: conn
    } do
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      attempt = latest_attempt()

      html = solve_through(view, attempt, 0..(length(attempt.steps) - 1))

      assert html =~ "Nice! Exercise solved unaided."
      assert html =~ "Next exercise"
      assert html =~ "1/5 before level 2"
    end

    test "a wrong answer shows the feedback panel with the try counter", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      attempt = latest_attempt()
      [first | _rest] = attempt.steps

      html = submit_step(view, wrong_params(first.step))

      assert html =~ "Try 1/3"
      assert html =~ "step-dot-error"
      refute html =~ "Show me the answer"
    end

    test "the reveal button appears at the threshold; revealing shows the expected value", %{
      conn: conn
    } do
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      attempt = latest_attempt()
      [first | _rest] = attempt.steps

      submit_step(view, wrong_params(first.step))
      submit_step(view, wrong_params(first.step))
      html = submit_step(view, wrong_params(first.step))

      assert html =~ "Try 3/3"
      assert html =~ "Show me the answer"

      html = view |> element("button", "Show me the answer") |> render_click()
      assert html =~ "Revealed"

      # Finishing the rest correctly still finalizes as "with help"
      html = solve_through(view, attempt, 1..(length(attempt.steps) - 1))
      assert html =~ "Exercise done with help."
    end

    test "next regenerates a fresh attempt", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      first_attempt = latest_attempt()

      solve_through(view, first_attempt, 0..(length(first_attempt.steps) - 1))
      html = view |> element("button", "Next exercise") |> render_click()

      refute html =~ "Nice! Exercise solved unaided."
      assert latest_attempt().id != first_attempt.id
    end

    test "skip abandons the current attempt for a fresh one", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      first_attempt = latest_attempt()

      view |> element("button", "Skip") |> render_click()

      assert latest_attempt().id != first_attempt.id
    end
  end

  describe "resume" do
    test "an in-progress attempt offers the resume banner and can be continued", %{conn: conn} do
      # First visit: answer one step correctly, then leave.
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      attempt = latest_attempt()
      submit_step(view, correct_params(hd(attempt.steps).step))

      # Second visit: same conn (same token cookie) -> banner.
      {:ok, view, html} = live(conn, "/exercise/encode/utf-8")
      assert html =~ "You have an exercise in progress"

      html = view |> element("button", "Continue") |> render_click()
      refute html =~ "You have an exercise in progress"
      # Step 1 already solved, step 2 active
      assert html =~ "Solved"
      assert latest_attempt().id == attempt.id
    end

    test "discarding the banner generates a new attempt", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      attempt = latest_attempt()
      submit_step(view, correct_params(hd(attempt.steps).step))

      {:ok, view, _html} = live(conn, "/exercise/encode/utf-8")
      view |> element("button", "New exercise") |> render_click()

      assert latest_attempt().id != attempt.id
    end

    test "a zero-progress attempt is adopted silently (no banner)", %{conn: conn} do
      {:ok, _view, _html} = live(conn, "/exercise/encode/utf-8")
      attempt = latest_attempt()

      {:ok, _view, html} = live(conn, "/exercise/encode/utf-8")

      refute html =~ "You have an exercise in progress"
      assert latest_attempt().id == attempt.id
    end
  end

  describe "localization" do
    test "renders in French under /fr", %{conn: conn} do
      {:ok, _view, html} = live(conn, "/fr/exercise/encode/utf-8")

      assert html =~ ~s(lang="fr")
      assert html =~ "Encode ce code point en UTF-8."
    end

    test "feedback hints are translated", %{conn: conn} do
      {:ok, view, _html} = live(conn, "/fr/exercise/encode/utf-8")
      attempt = latest_attempt()

      html = submit_step(view, wrong_params(hd(attempt.steps).step))

      assert html =~ "Essai 1/3"
    end
  end

  test "utf-16 attempts show the byte order in the prompt", %{conn: conn} do
    {:ok, _view, html} = live(conn, "/exercise/encode/utf-16")

    assert html =~ "Byte order"
    assert html =~ ~r/(Big|Little)-endian/
  end
end
