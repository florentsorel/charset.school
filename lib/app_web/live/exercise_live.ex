defmodule AppWeb.ExerciseLive do
  @moduledoc """
  The exercise page: one LiveView for the 6 playable modules (routed via the
  live action), driving `Charset.Exercise.Service`.

  Anti-cheat by construction: the step structs (with their `expected`) live
  in the socket assigns and the DB - the template only renders public
  structure (lengths, counts, choices), plus the expected value of steps the
  user explicitly revealed. Generation only happens on the connected mount,
  so crawlers and dead renders never create attempts.
  """
  use AppWeb, :live_view

  import AppWeb.ExerciseComponents

  alias Charset.Exercise.AttemptStep
  alias Charset.Exercise.ExerciseModule
  alias Charset.Exercise.Service
  alias Charset.Exercise.Step
  alias Charset.Progress

  @modules %{
    utf8_encode: {"encode", "utf-8"},
    utf8_decode: {"decode", "utf-8"},
    utf16_encode: {"encode", "utf-16"},
    utf16_decode: {"decode", "utf-16"},
    utf32_encode: {"encode", "utf-32"},
    utf32_decode: {"decode", "utf-32"}
  }

  @impl true
  def mount(_params, session, socket) do
    module = socket.assigns.live_action
    {direction, encoding_slug} = Map.fetch!(@modules, module)
    token = session["visitor_token"]

    progress = Progress.find(token, module)
    resumable = Service.find_resumable(token, module)

    socket =
      socket
      |> assign(
        token: token,
        module: module,
        direction: direction,
        encoding_slug: encoding_slug,
        max_level: ExerciseModule.max_level(module),
        threshold: Service.reveal_threshold(),
        level: (progress && progress.level) || 1,
        streak: (progress && progress.streak) || 0,
        page_title: module_title(module),
        attempt: nil,
        pending_resume: nil,
        current_index: 0,
        last_validation: nil,
        finalized: false,
        finalized_correct: false
      )
      |> bootstrap(resumable)

    {:ok, socket}
  end

  # Resumable with real progress -> offer the banner. Zero-progress (just
  # generated, never touched) -> adopt silently: resuming would be identical
  # and the banner would read as a misleading "exercise in progress". None ->
  # generate, but only once connected (never during the dead render).
  defp bootstrap(socket, resumable) do
    cond do
      resumable && resolved_count(resumable) > 0 ->
        assign(socket, :pending_resume, resumable)

      resumable ->
        adopt(socket, resumable)

      connected?(socket) ->
        adopt(socket, Service.generate(socket.assigns.token, socket.assigns.module))

      true ->
        socket
    end
  end

  defp adopt(socket, attempt) do
    socket
    |> assign(:attempt, attempt)
    |> assign(:pending_resume, nil)
    |> assign(:current_index, first_unresolved(attempt))
    |> assign(:last_validation, seed_validation(attempt))
    |> assign(:finalized, attempt.finalized)
    |> assign(:finalized_correct, attempt.correct)
  end

  defp resolved_count(attempt), do: Enum.count(attempt.steps, &AttemptStep.resolved?/1)

  defp first_unresolved(attempt) do
    case Enum.find_index(attempt.steps, &(not AttemptStep.resolved?(&1))) do
      nil -> length(attempt.steps) - 1
      index -> index
    end
  end

  # A resumed active step keeps its attempt counter and last error type (the
  # params aren't persisted - the hint renders without interpolation context,
  # same as the old frontend).
  defp seed_validation(attempt) do
    step = Enum.at(attempt.steps, first_unresolved(attempt))

    if step && not AttemptStep.resolved?(step) && step.attempts > 0 && step.error_type do
      validation_state(step, step.error_type, %{})
    end
  end

  defp validation_state(step, error_type, params) do
    %{
      error_type: error_type,
      params: params,
      attempts: step.attempts,
      can_reveal: step.attempts >= Service.reveal_threshold()
    }
  end

  ## Events

  @impl true
  def handle_event("submit-step", params, socket) do
    %{attempt: attempt, current_index: index, token: token} = socket.assigns
    step = Enum.at(attempt.steps, index)

    case build_answer(step.step, params) do
      :incomplete ->
        {:noreply, socket}

      {:ok, answer} ->
        case Service.validate_step(token, attempt.id, index, answer) do
          {:ok, outcome} ->
            {:noreply, apply_validation(socket, outcome)}

          {:error, _stale} ->
            {:noreply, resync(socket)}
        end
    end
  end

  def handle_event("reveal", _params, socket) do
    %{attempt: attempt, current_index: index, token: token} = socket.assigns

    case Service.reveal_step(token, attempt.id, index) do
      {:ok, outcome} ->
        socket =
          socket
          |> assign(:attempt, outcome.attempt)
          |> assign(:last_validation, nil)
          |> assign(:current_index, first_unresolved(outcome.attempt))
          |> finalize_if(outcome.attempt)

        {:noreply, socket}

      {:error, _stale} ->
        {:noreply, resync(socket)}
    end
  end

  def handle_event("resume", _params, socket) do
    case socket.assigns.pending_resume do
      nil -> {:noreply, socket}
      pending -> {:noreply, socket |> assign(:level, pending.level) |> adopt(pending)}
    end
  end

  def handle_event("discard-resume", _params, socket) do
    {:noreply, generate_fresh(socket)}
  end

  def handle_event("skip", _params, socket) do
    {:noreply, generate_fresh(socket)}
  end

  def handle_event("next", _params, socket) do
    {:noreply, generate_fresh(socket)}
  end

  defp generate_fresh(socket) do
    socket
    |> adopt(Service.generate(socket.assigns.token, socket.assigns.module))
    |> refresh_progress()
  end

  defp apply_validation(socket, outcome) do
    socket =
      socket
      |> assign(:attempt, outcome.attempt)
      |> assign(:finalized, outcome.finalized)
      |> assign(:finalized_correct, outcome.attempt.correct)

    socket =
      if outcome.validation.ok do
        socket
        |> assign(:last_validation, nil)
        |> assign(:current_index, first_unresolved(outcome.attempt))
      else
        assign(
          socket,
          :last_validation,
          validation_state(outcome.step, outcome.validation.error_type, outcome.validation.params)
        )
      end

    if outcome.finalized, do: refresh_progress(socket), else: socket
  end

  defp finalize_if(socket, attempt) do
    socket =
      socket
      |> assign(:finalized, attempt.finalized)
      |> assign(:finalized_correct, attempt.correct)

    if attempt.finalized, do: refresh_progress(socket), else: socket
  end

  defp resync(socket) do
    case Charset.ExerciseAttempts.get(socket.assigns.attempt.id) do
      nil -> socket
      attempt -> adopt(socket, attempt)
    end
  end

  defp refresh_progress(socket) do
    case Progress.find(socket.assigns.token, socket.assigns.module) do
      nil -> socket
      progress -> assign(socket, level: progress.level, streak: progress.streak)
    end
  end

  ## Answer building (form params -> Answer tuples)

  defp build_answer(%Step.Format{}, %{"value" => value}) when value != "",
    do: {:ok, {:format, value}}

  defp build_answer(%Step.Format{}, _params), do: :incomplete

  defp build_answer(%Step.Binary{} = step, params) do
    # Disabled locked cells (decode padding) don't post - re-add their zeros.
    locked = step.length - length(Map.get(params, "bits", []))
    cells = List.duplicate("0", max(locked, 0)) ++ Map.get(params, "bits", [])
    {:ok, {:binary, join_cells(cells)}}
  end

  defp build_answer(%Step.BitGroups{}, %{"groups" => groups}) do
    joined =
      groups
      |> Enum.sort_by(fn {key, _cells} -> String.to_integer(key) end)
      |> Enum.map(fn {_key, cells} -> join_cells(cells) end)

    {:ok, {:bit_groups, joined}}
  end

  defp build_answer(%Step.HexBytes{}, %{"bytes" => cells}) do
    if Enum.all?(cells, &(&1 =~ ~r/^[0-9a-fA-F]{2}$/)) do
      {:ok, {:hex_bytes, Enum.map(cells, &String.to_integer(&1, 16))}}
    else
      :incomplete
    end
  end

  defp build_answer(%Step.CodePointEntry{}, %{"value" => value}),
    do: parse_hex_answer(value, :code_point)

  defp build_answer(%Step.Offset{}, %{"value" => value}), do: parse_hex_answer(value, :offset)

  defp build_answer(%Step.UsefulBitCount{}, %{"value" => value}) do
    case Integer.parse(value) do
      {count, ""} -> {:ok, {:useful_bit_count, count}}
      _other -> :incomplete
    end
  end

  defp build_answer(_step, _params), do: :incomplete

  defp parse_hex_answer(value, tag) do
    case Integer.parse(String.trim(value), 16) do
      {parsed, ""} -> {:ok, {tag, parsed}}
      _other -> :incomplete
    end
  end

  # Empty cells become spaces (so position information survives), then the
  # trailing run is trimmed - same shape the old BitInput emitted.
  defp join_cells(cells) do
    cells
    |> Enum.map_join(fn
      "" -> " "
      char -> char
    end)
    |> String.trim_trailing()
  end

  ## View helpers

  defp module_title(:utf8_encode), do: gettext("Charset Playground - Encode UTF-8")
  defp module_title(:utf8_decode), do: gettext("Charset Playground - Decode UTF-8")
  defp module_title(:utf16_encode), do: gettext("Charset Playground - Encode UTF-16")
  defp module_title(:utf16_decode), do: gettext("Charset Playground - Decode UTF-16")
  defp module_title(:utf32_encode), do: gettext("Charset Playground - Encode UTF-32")
  defp module_title(:utf32_decode), do: gettext("Charset Playground - Decode UTF-32")

  defp module_prompt(:utf8_encode), do: gettext("Encode this code point in UTF-8.")
  defp module_prompt(:utf8_decode), do: gettext("Decode these UTF-8 bytes into a code point.")
  defp module_prompt(:utf16_encode), do: gettext("Encode this code point in UTF-16.")
  defp module_prompt(:utf16_decode), do: gettext("Decode these UTF-16 bytes into a code point.")
  defp module_prompt(:utf32_encode), do: gettext("Encode this code point in UTF-32.")
  defp module_prompt(:utf32_decode), do: gettext("Decode these UTF-32 bytes into a code point.")

  # UTF-16/32 carry a byte order: the exercise gives it in the prompt header
  # rather than asking the user to derive it.
  defp endianness(%{encoding: encoding}) do
    case encoding do
      e when e in [:utf16be, :utf32be] -> :big
      e when e in [:utf16le, :utf32le] -> :little
      _single_order -> nil
    end
  end

  defp endianness_text(:big), do: gettext("Big-endian · BE")
  defp endianness_text(:little), do: gettext("Little-endian · LE")

  # In the UTF-8 module, flag which legacy single-byte encoding also covers
  # this code point - the divergence behind mojibake. Above 0xFF: no badge.
  defp legacy_badge(module, code_point) do
    cond do
      module not in [:utf8_encode, :utf8_decode] -> nil
      code_point <= 0x7F -> :ascii
      code_point <= 0xFF -> :latin1
      true -> nil
    end
  end

  defp legacy_badge_text(:ascii), do: gettext("ASCII")
  defp legacy_badge_text(:latin1), do: gettext("Latin-1")

  defp legacy_badge_hint(:ascii),
    do: gettext("ASCII range (U+0000–U+007F): 1 byte, identical in ASCII, Latin-1 and UTF-8.")

  defp legacy_badge_hint(:latin1),
    do: gettext("Latin-1 range (U+0080–U+00FF): 1 byte in Latin-1, but 2 bytes in UTF-8.")

  # Some steps read differently per direction: the offset step subtracts
  # (encode) vs reads the binary as hex (decode); the UTF-16 decode binary
  # step assembles the two halves; and the decode code-point step is where
  # 0x10000 is added back (the offset step's presence marks that case).
  defp step_title(step, assigns) do
    has_offset = Enum.any?(assigns.attempt.steps, &match?(%Step.Offset{}, &1.step))
    utf16? = assigns.module in [:utf16_encode, :utf16_decode]

    case step do
      %Step.Format{} ->
        gettext("Pick the format")

      %Step.Binary{} when assigns.direction == "decode" and utf16? ->
        gettext("Assemble the 20-bit value")

      %Step.Binary{} ->
        gettext("Convert to binary")

      %Step.BitGroups{} ->
        gettext("Split the useful bits (MSB / LSB)")

      %Step.HexBytes{} ->
        gettext("Convert to hex")

      %Step.CodePointEntry{} when has_offset ->
        gettext("Identify the code point (add 0x10000)")

      %Step.CodePointEntry{} ->
        gettext("Identify the code point")

      %Step.UsefulBitCount{} ->
        gettext("How many useful bits?")

      %Step.Offset{} when assigns.direction == "decode" ->
        gettext("Convert to hex")

      %Step.Offset{} ->
        gettext("Subtract 0x10000")

      %Step.Endianness{} ->
        gettext("Pick the endianness")
    end
  end

  # Binary grouping separator: UTF-8/32 are byte-aligned (8); UTF-16 groups
  # by nibble (4) - the value is a hex number (cp - 0x10000), so 4-bit groups
  # line up with the hex digits. The 10|10 split shows at the bit-groups step.
  defp binary_boundary(module) when module in [:utf16_encode, :utf16_decode], do: 4
  defp binary_boundary(_module), do: 8

  # UTF-8 marker pattern per byte (leader 1{n}0, continuations 10); UTF-16
  # supplementary surrogate markers (110110 / 110111). Bare groups elsewhere.
  defp bit_group_markers(module, group_lengths) do
    cond do
      module in [:utf8_encode, :utf8_decode] ->
        byte_count = length(group_lengths)

        if byte_count <= 1 do
          []
        else
          [String.duplicate("1", byte_count) <> "0" | List.duplicate("10", byte_count - 1)]
        end

      module in [:utf16_encode, :utf16_decode] and length(group_lengths) == 2 ->
        ["110110", "110111"]

      true ->
        []
    end
  end

  # The two UTF-16 surrogate markers ARE the bases 0xD800/0xDC00 - labelling
  # them makes explicit that the payload is added onto these bases.
  defp bit_group_labels(module, group_lengths) do
    if module in [:utf16_encode, :utf16_decode] and length(group_lengths) == 2 do
      ["0xD800", "0xDC00"]
    else
      []
    end
  end

  # In a decode exercise the binary step's leading bits are pure
  # byte-alignment padding (zeros): pre-filled and locked instead of typed.
  # Encode keeps the padding manual (the binary step comes before the count).
  defp binary_locked_prefix(assigns, %Step.Binary{} = step) do
    with "decode" <- assigns.direction,
         %Step.BitGroups{} = bit_groups <-
           Enum.find_value(
             assigns.attempt.steps,
             &(match?(%Step.BitGroups{}, &1.step) && &1.step)
           ) do
      useful = bit_groups.expected |> Enum.map(&String.length/1) |> Enum.sum()
      max(step.length - useful, 0)
    else
      _other -> 0
    end
  end

  # Colour the resolved binary by group once the user has reached the
  # bit-groups step. UTF-16 stays neutral (its binary is nibble-grouped; the
  # 10|10 split is coloured at the bit-groups step instead).
  defp binary_segments(assigns, index) do
    step = Enum.at(assigns.attempt.steps, index).step
    bg_index = Enum.find_index(assigns.attempt.steps, &match?(%Step.BitGroups{}, &1.step))

    with false <- assigns.module in [:utf16_encode, :utf16_decode],
         %Step.Binary{} <- step,
         true <- bg_index != nil and assigns.current_index >= bg_index do
      group_lengths = group_lengths_at(assigns, bg_index)
      useful = Enum.sum(group_lengths)
      padding = step.length - useful
      palette = [:payload, :marker, :cont, :boundary]

      segments =
        group_lengths
        |> Enum.with_index()
        |> Enum.map(fn {length, i} -> {length, Enum.at(palette, rem(i, 4))} end)

      if padding > 0, do: [{padding, :plain} | segments], else: segments
    else
      _other -> nil
    end
  end

  defp group_lengths_at(assigns, index) do
    %Step.BitGroups{expected: expected} = Enum.at(assigns.attempt.steps, index).step
    Enum.map(expected, &String.length/1)
  end

  defp group_lengths(%Step.BitGroups{expected: expected}),
    do: Enum.map(expected, &String.length/1)

  defp step_state(assigns, index) do
    step = Enum.at(assigns.attempt.steps, index)

    cond do
      AttemptStep.resolved?(step) -> :done
      index != assigns.current_index -> :todo
      assigns.last_validation -> :error
      true -> :active
    end
  end

  # The value shown for a resolved step: the revealed expected when the user
  # gave up, their own answer otherwise. Revealing is the ONLY path that
  # renders an expected value.
  defp resolved_value(%AttemptStep{revealed: true, step: step}), do: expected_answer(step)
  defp resolved_value(%AttemptStep{user_answer: answer}), do: answer

  defp expected_answer(%Step.Format{expected: expected}), do: {:format, expected}
  defp expected_answer(%Step.Binary{expected: expected}), do: {:binary, expected}
  defp expected_answer(%Step.BitGroups{expected: expected}), do: {:bit_groups, expected}
  defp expected_answer(%Step.HexBytes{expected: expected}), do: {:hex_bytes, expected}
  defp expected_answer(%Step.CodePointEntry{expected: expected}), do: {:code_point, expected}

  defp expected_answer(%Step.UsefulBitCount{expected: expected}),
    do: {:useful_bit_count, expected}

  defp expected_answer(%Step.Offset{expected: expected}), do: {:offset, expected}
  defp expected_answer(%Step.Endianness{expected: expected}), do: {:endianness, expected}

  defp pad2(n), do: String.pad_leading(Integer.to_string(n), 2, "0")

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.app flash={@flash} locale={@locale} alternate_path={@alternate_path}>
      <div class="exercise-page">
        <.exercise_sub_header
          direction={@direction}
          encoding_slug={@encoding_slug}
          level={@level}
          max_level={@max_level}
          streak={@streak}
          threshold={Charset.Progress.ModuleProgress.streak_for_level_up()}
        />

        <div class="exercise-container">
          <div
            :if={@pending_resume}
            class="exercise-resume-banner banner-accent"
            role="region"
            aria-label={gettext("Exercise in progress")}
          >
            <p class="exercise-resume-message">
              <%= if @pending_resume.level >= @max_level do %>
                {gettext("You have an exercise in progress - level %{level}.",
                  level: @pending_resume.level
                )}
              <% else %>
                {gettext("You have an exercise in progress - level %{level}, step %{done}/%{total}.",
                  level: @pending_resume.level,
                  done: resolved_count(@pending_resume),
                  total: length(@pending_resume.steps)
                )}
              <% end %>
            </p>
            <div class="exercise-resume-actions">
              <button type="button" class="btn btn-primary" phx-click="resume">
                {gettext("Continue")}
              </button>
              <button type="button" class="btn btn-ghost" phx-click="discard-resume">
                {gettext("New exercise")}
              </button>
            </div>
          </div>

          <header :if={!@pending_resume} class="exercise-prompt">
            <p class="exercise-prompt-tag">{gettext("Exercise")}</p>
            <h1 class="exercise-prompt-title">{module_prompt(@module)}</h1>

            <div :if={@attempt && @direction == "encode"} class="exercise-prompt-card surface-subtle">
              <div>
                <p class="exercise-prompt-card-label">{gettext("Code point")}</p>
                <div class="exercise-prompt-card-content">
                  <span class="codepoint-glyph">{code_point_text(@attempt.code_point)}</span>
                  <abbr
                    :if={badge = legacy_badge(@module, @attempt.code_point)}
                    class="legacy-badge"
                    title={legacy_badge_hint(badge)}
                  >
                    {legacy_badge_text(badge)}
                  </abbr>
                </div>
              </div>
              <div :if={endian = endianness(@attempt)}>
                <p class="exercise-prompt-card-label">{gettext("Byte order")}</p>
                <div class="exercise-prompt-card-content">
                  <span class="endianness-value">{endianness_text(endian)}</span>
                </div>
              </div>
            </div>

            <div :if={@attempt && @direction == "decode"} class="exercise-prompt-card surface-subtle">
              <div>
                <p class="exercise-prompt-card-label">{gettext("Bytes")}</p>
                <div class="exercise-prompt-card-content">
                  <span :for={byte <- attempt_bytes(@attempt)} class="byte-display">
                    {byte_text(byte)}
                  </span>
                </div>
              </div>
              <div :if={endian = endianness(@attempt)}>
                <p class="exercise-prompt-card-label">{gettext("Byte order")}</p>
                <div class="exercise-prompt-card-content">
                  <span class="endianness-value">{endianness_text(endian)}</span>
                </div>
              </div>
            </div>
          </header>

          <ol :if={@attempt && !@pending_resume} class="exercise-steps">
            <li :for={{attempt_step, index} <- Enum.with_index(@attempt.steps)} class="exercise-step">
              <div class="exercise-step-track">
                <span class={["step-dot", "step-dot-#{step_state(assigns, index)}"]}>
                  <svg
                    :if={step_state(assigns, index) == :done}
                    width="12"
                    height="12"
                    viewBox="0 0 12 12"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M2.5 6.2l2.5 2.3L9.5 3.5" />
                  </svg>
                  <svg
                    :if={step_state(assigns, index) == :error}
                    width="11"
                    height="11"
                    viewBox="0 0 11 11"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M3 3l5 5M8 3l-5 5" />
                  </svg>
                  <span :if={step_state(assigns, index) in [:active, :todo]}>{pad2(index + 1)}</span>
                </span>
                <span
                  :if={index < length(@attempt.steps) - 1}
                  class={[
                    "step-connector",
                    step_state(assigns, index) == :done && "step-connector-done"
                  ]}
                >
                </span>
              </div>

              <div class="exercise-step-content">
                <div class="exercise-step-header">
                  <span class="exercise-step-index">{pad2(index + 1)}</span>
                  <h2 class="exercise-step-title">{step_title(attempt_step.step, assigns)}</h2>
                </div>

                <form
                  :if={
                    index == @current_index and not @finalized and
                      not AttemptStep.resolved?(attempt_step)
                  }
                  class="exercise-step-input surface"
                  phx-submit="submit-step"
                >
                  <.step_widget
                    attempt_step={attempt_step}
                    index={index}
                    attempt_id={@attempt.id}
                    module={@module}
                    page_assigns={assigns}
                  />

                  <.feedback_panel
                    :if={@last_validation}
                    error_type={@last_validation.error_type}
                    params={@last_validation.params}
                    attempts={@last_validation.attempts}
                    can_reveal={@last_validation.can_reveal}
                    threshold={@threshold}
                  />

                  <div class="exercise-step-actions">
                    <button type="submit" class="btn btn-primary">
                      {gettext("Submit")}
                    </button>
                  </div>
                </form>

                <div :if={AttemptStep.resolved?(attempt_step)} class="exercise-step-resolved">
                  <div class="exercise-step-resolved-header">
                    <span class={[
                      "exercise-step-resolved-tag",
                      attempt_step.revealed && "exercise-step-resolved-tag-revealed"
                    ]}>
                      {(attempt_step.revealed && gettext("Revealed")) || gettext("Solved")}
                    </span>
                  </div>
                  <div class="exercise-step-resolved-answer">
                    <.resolved_answer
                      attempt_step={attempt_step}
                      index={index}
                      module={@module}
                      page_assigns={assigns}
                    />
                  </div>
                </div>
              </div>
            </li>
          </ol>

          <div :if={@finalized} class="exercise-finalized surface-subtle">
            <p class="exercise-finalized-message">
              <%= if @finalized_correct do %>
                {gettext("Nice! Exercise solved unaided.")}
              <% else %>
                {gettext(
                  "Exercise done with help. Try again without revealing to solve it on your own!"
                )}
              <% end %>
            </p>
            <div class="exercise-finalized-actions">
              <button type="button" class="btn btn-primary" phx-click="next">
                {gettext("Next exercise")}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Layouts.app>
    """
  end

  # Decode prompts show the byte sequence; it is derived (not persisted) -
  # the attempt's code point encoded in the attempt's encoding.
  defp attempt_bytes(attempt) do
    :binary.bin_to_list(Charset.Encoding.Codec.encode!(attempt.code_point, attempt.encoding))
  end

  # The active step's input widget. Ids carry the attempt + step index so a
  # new exercise or step remounts the ignored containers.
  defp step_widget(%{attempt_step: %{step: step}} = assigns) do
    assigns = assign(assigns, :step, step)
    assigns = assign(assigns, :widget_id, "step-#{assigns.attempt_id}-#{assigns.index}")

    ~H"""
    <%= case @step do %>
      <% %Step.Format{} -> %>
        <.format_selector choices={@step.choices} />
      <% %Step.Binary{} -> %>
        <.bit_input
          id={@widget_id}
          length={@step.length}
          boundary_every={binary_boundary(@module)}
          locked_prefix={binary_locked_prefix(@page_assigns, @step)}
        />
      <% %Step.BitGroups{} -> %>
        <.bit_groups_input
          id={@widget_id}
          group_lengths={group_lengths(@step)}
          markers={bit_group_markers(@module, group_lengths(@step))}
          labels={bit_group_labels(@module, group_lengths(@step))}
        />
      <% %Step.HexBytes{} -> %>
        <.hex_input id={@widget_id} byte_count={length(@step.expected)} />
      <% %Step.CodePointEntry{} -> %>
        <.code_point_input id={@widget_id} />
      <% %Step.UsefulBitCount{} -> %>
        <.useful_bit_count_input id={@widget_id} />
      <% %Step.Offset{} -> %>
        <.offset_input id={@widget_id} />
    <% end %>
    """
  end

  defp resolved_answer(%{attempt_step: attempt_step} = assigns) do
    assigns = assign(assigns, :answer, resolved_value(attempt_step))

    ~H"""
    <%= case @answer do %>
      <% {:format, value} -> %>
        <span class="font-mono text-sm">{format_choice_text(value)}</span>
      <% {:binary, bits} -> %>
        <.bit_display
          bits={bits}
          boundary_every={binary_boundary(@module)}
          segments={binary_segments(@page_assigns, @index)}
        />
      <% {:bit_groups, groups} -> %>
        <span class="bit-groups-display">
          <span :for={{group, gi} <- Enum.with_index(groups)} class="bit-groups-byte-resolved">
            <span class="bit-groups-byte-cells">
              <span
                :if={
                  marker =
                    Enum.at(
                      bit_group_markers(@module, Enum.map(groups, &String.length/1)),
                      gi
                    )
                }
                class="bit-row"
              >
                <span
                  :for={char <- String.graphemes(marker)}
                  class={["bit", (gi == 0 && "bit-marker") || "bit-cont"]}
                >
                  {char}
                </span>
              </span>
              <.bit_display bits={group} />
            </span>
            <span
              :if={label = Enum.at(bit_group_labels(@module, Enum.map(groups, &String.length/1)), gi)}
              class="bit-groups-byte-label"
            >
              {label}
            </span>
          </span>
        </span>
      <% {:hex_bytes, bytes} -> %>
        <span class="bytes-display">
          <span :for={byte <- bytes} class="byte-display">{byte_text(byte)}</span>
        </span>
      <% {:code_point, value} -> %>
        <span class="codepoint-glyph">{code_point_text(value)}</span>
        <abbr
          :if={badge = legacy_badge(@module, value)}
          class="legacy-badge"
          title={legacy_badge_hint(badge)}
        >
          {legacy_badge_text(badge)}
        </abbr>
      <% {:useful_bit_count, count} -> %>
        <span class="font-mono text-sm">{count} {gettext("useful bits")}</span>
      <% {:offset, value} -> %>
        <span class="font-mono text-sm">0x{Integer.to_string(value, 16)}</span>
      <% {:endianness, value} -> %>
        <span class="font-mono text-sm">{endianness_text(value)}</span>
      <% nil -> %>
        <span class="font-mono text-sm">-</span>
    <% end %>
    """
  end
end
