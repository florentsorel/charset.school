defmodule AppWeb.ExerciseComponents do
  @moduledoc """
  Function components of the exercise page: the sub-header, the step input
  widgets (cell-based inputs driven by the JS hooks in exercise_hooks.js),
  the feedback panel and the resolved-answer displays.

  Anti-cheat rule: widgets only ever receive PUBLIC structure (lengths,
  counts, choices) - never a step's `expected` value.
  """

  use Phoenix.Component
  use Gettext, backend: AppWeb.Gettext

  import AppWeb.SandboxComponents, only: [inline_desc: 1]

  @doc """
  Breadcrumb sub-header: direction / encoding / progression pill + skip.
  """
  attr :direction, :string, required: true
  attr :encoding_slug, :string, required: true
  attr :level, :integer, required: true
  attr :max_level, :integer, required: true
  attr :streak, :integer, required: true
  attr :threshold, :integer, required: true

  def exercise_sub_header(assigns) do
    ~H"""
    <div class="exercise-sub-header">
      <%!-- Same container utilities as the app header so the breadcrumb
           lines up with the logo by construction. --%>
      <div class="exercise-sub-header-inner mx-auto max-w-6xl px-4 sm:px-6">
        <nav class="exercise-breadcrumb" aria-label={gettext("Breadcrumb")}>
          <span class="crumb-root">{gettext("Exercises")}</span>
          <span class="separator">/</span>
          <span class="crumb font-mono">{@direction}</span>
          <span class="separator">/</span>
          <span class="crumb font-mono">{@encoding_slug}</span>
          <span class="separator">/</span>
          <span :if={@level >= @max_level} class="crumb crumb-level font-mono">
            {gettext("Level %{n} · mastered", n: @level)}
          </span>
          <span :if={@level < @max_level} class="crumb crumb-level font-mono">
            {gettext("Level %{level} · %{done}/%{threshold} before level %{next}",
              level: @level,
              done: @streak,
              threshold: @threshold,
              next: @level + 1
            )}
          </span>
        </nav>
        <div class="exercise-stats">
          <button type="button" class="btn-quiet btn" phx-click="skip">
            {gettext("Skip")}
          </button>
        </div>
      </div>
    </div>
    """
  end

  @doc """
  Format choice cards as styled radio inputs (name `value`).
  """
  attr :choices, :list, required: true
  attr :selected, :string, default: nil

  def format_selector(assigns) do
    ~H"""
    <div class="format-grid">
      <label :for={choice <- @choices} class="format-card">
        <input type="radio" name="value" value={choice} checked={@selected == choice} class="sr-only" />
        <span class="format-card-title">{format_choice_text(choice)}</span>
      </label>
    </div>
    """
  end

  @doc """
  Cell-based binary input (one 0/1 per cell), wrapping at `wrap_every`,
  with a byte/nibble separator every `boundary_every` cells and an optional
  locked all-zero prefix (the decode padding). Field name: `bits[]`.
  """
  attr :id, :string, required: true
  attr :length, :integer, required: true
  attr :boundary_every, :integer, default: 0
  attr :locked_prefix, :integer, default: 0
  attr :prefill, :list, default: [], doc: "initial cell values (resume)"

  def bit_input(assigns) do
    assigns = assign(assigns, :rows, bit_rows(assigns.length, assigns.boundary_every))

    ~H"""
    <span class="bit-rows" id={@id} phx-hook="BitCells" phx-update="ignore">
      <span :for={row <- @rows} class="bit-row bit-row-nowrap">
        <%= for {index, cell_pos} <- Enum.with_index(row) do %>
          <span
            :if={@boundary_every > 0 and cell_pos > 0 and rem(cell_pos, @boundary_every) == 0}
            class="bit-sep-mid"
          >
          </span>
          <input
            class={["bit bit-input", index < @locked_prefix && "bit-input-locked"]}
            type="text"
            inputmode="numeric"
            maxlength="1"
            name="bits[]"
            value={bit_cell_value(@prefill, index, @locked_prefix)}
            disabled={index < @locked_prefix}
            aria-label={gettext("Bit %{n}", n: index + 1)}
          />
        <% end %>
      </span>
    </span>
    """
  end

  # Disabled inputs don't post; the server re-adds the locked zero prefix.
  defp bit_cell_value(prefill, index, locked_prefix) do
    cond do
      index < locked_prefix -> "0"
      value = Enum.at(prefill, index) -> value
      true -> nil
    end
  end

  defp bit_rows(length, boundary_every) do
    # One line when the value fits (<= 20 bits, e.g. UTF-16), else wrap at
    # two boundaries per row (UTF-32 32 -> 16|16, UTF-8 4-byte 24 -> 16|8).
    wrap =
      cond do
        boundary_every <= 0 -> length
        length <= 20 -> length
        true -> boundary_every * 2
      end

    0..(length - 1) |> Enum.chunk_every(wrap)
  end

  @doc """
  Bit groups input: one cell-based group per packet, optional fixed marker
  prefixes (UTF-8 leaders/continuations, UTF-16 surrogate markers) and
  per-group captions (the surrogate bases). One hook root so focus flows
  across groups. Field names: `groups[N][]`.
  """
  attr :id, :string, required: true
  attr :group_lengths, :list, required: true
  attr :markers, :list, default: []
  attr :labels, :list, default: []
  attr :prefill, :list, default: [], doc: "initial group strings (resume)"

  def bit_groups_input(assigns) do
    ~H"""
    <div class="bit-groups-input" id={@id} phx-hook="BitCells" phx-update="ignore">
      <div :for={{length, group_index} <- Enum.with_index(@group_lengths)} class="bit-groups-byte">
        <span class="bit-groups-byte-cells">
          <span :if={marker = Enum.at(@markers, group_index)} class="bit-row">
            <span
              :for={char <- String.graphemes(marker)}
              class={["bit", (group_index == 0 && "bit-marker") || "bit-cont"]}
            >
              {char}
            </span>
          </span>
          <span class="bit-row bit-row-nowrap">
            <input
              :for={cell <- 0..(length - 1)}
              class="bit bit-input"
              type="text"
              inputmode="numeric"
              maxlength="1"
              name={"groups[#{group_index}][]"}
              value={group_prefill_cell(@prefill, group_index, cell)}
              aria-label={gettext("Bit %{n}", n: cell + 1)}
            />
          </span>
        </span>
        <span :if={label = Enum.at(@labels, group_index)} class="bit-groups-byte-label">
          {label}
        </span>
      </div>
    </div>
    """
  end

  defp group_prefill_cell(prefill, group_index, cell) do
    case prefill |> Enum.at(group_index, "") |> String.at(cell) do
      nil -> nil
      char -> char
    end
  end

  @doc """
  Hex byte cells (two hex chars per cell). Field name: `bytes[]`.
  """
  attr :id, :string, required: true
  attr :byte_count, :integer, required: true
  attr :prefill, :list, default: []

  def hex_input(assigns) do
    ~H"""
    <span class="hex-row" id={@id} phx-hook="HexCells" phx-update="ignore">
      <input
        :for={index <- 0..(@byte_count - 1)}
        class="hex-cell"
        type="text"
        inputmode="text"
        maxlength="2"
        autocapitalize="characters"
        name="bytes[]"
        value={Enum.at(@prefill, index)}
        aria-label={gettext("Byte %{n}", n: index + 1)}
      />
    </span>
    """
  end

  @doc """
  Code point hex entry, with the `U+` prefix rendered outside the field.
  Field name: `value`.
  """
  attr :id, :string, required: true
  attr :prefill, :string, default: nil

  def code_point_input(assigns) do
    ~H"""
    <div class="offset-input" id={@id} phx-update="ignore">
      <span class="prefix">U+</span>
      <input
        class="offset-cell"
        type="text"
        inputmode="text"
        autocapitalize="characters"
        name="value"
        value={@prefill}
        data-filter="hex"
        phx-hook="FilteredInput"
        id={@id <> "-input"}
        aria-label={gettext("Enter the code point in hex")}
      />
    </div>
    """
  end

  @doc """
  Offset hex entry (the UTF-16 ±0x10000 step). Field name: `value`.
  """
  attr :id, :string, required: true
  attr :prefill, :string, default: nil

  def offset_input(assigns) do
    ~H"""
    <div class="offset-input" id={@id} phx-update="ignore">
      <span class="prefix">0x</span>
      <input
        class="offset-cell"
        type="text"
        inputmode="text"
        autocapitalize="characters"
        name="value"
        value={@prefill}
        data-filter="hex"
        phx-hook="FilteredInput"
        id={@id <> "-input"}
        aria-label={gettext("Enter the value in hex")}
      />
    </div>
    """
  end

  @doc """
  Useful bit count entry. Field name: `value`.
  """
  attr :id, :string, required: true
  attr :prefill, :string, default: nil

  def useful_bit_count_input(assigns) do
    ~H"""
    <div class="useful-bit-count-input" id={@id} phx-update="ignore">
      <input
        class="useful-bit-count-cell"
        type="text"
        inputmode="numeric"
        maxlength="2"
        name="value"
        value={@prefill}
        data-filter="digits"
        phx-hook="FilteredInput"
        id={@id <> "-input"}
        aria-label={gettext("Useful bit count")}
      />
      <span class="suffix">{gettext("useful bits")}</span>
    </div>
    """
  end

  @doc """
  The wrong-answer feedback: attempt counter, hint mapped from the stable
  error type, and the reveal button once the threshold is reached.
  """
  attr :error_type, :string, required: true
  attr :params, :map, required: true
  attr :attempts, :integer, required: true
  attr :can_reveal, :boolean, required: true
  attr :threshold, :integer, required: true

  def feedback_panel(assigns) do
    ~H"""
    <div class="feedback-panel" role="alert">
      <div class="feedback-header">
        <span class="feedback-tag">
          {gettext("Try %{n}/%{threshold}", n: @attempts, threshold: @threshold)}
        </span>
      </div>
      <p class="feedback-message">
        <.inline_desc text={hint_message(@error_type, @params)} />
      </p>
      <div :if={@can_reveal} class="feedback-actions">
        <button type="button" class="btn btn-ghost" phx-click="reveal">
          {gettext("Show me the answer")}
        </button>
      </div>
    </div>
    """
  end

  # Hints live in the "feedback" gettext domain, keyed by the stable error
  # type (closed list, dynamic lookup). Unknown types fall back to a generic
  # nudge so a new error type never renders a raw key.
  defp hint_message(error_type, params) do
    case Gettext.dgettext(AppWeb.Gettext, "feedback", error_type, feedback_bindings(params)) do
      ^error_type -> gettext("Not quite right. Try again.")
      message -> message
    end
  end

  # ValidationResult params use the stable hyphenated ParamKey strings;
  # Gettext bindings are atoms. Closed set - see Charset.Exercise.ParamKey.
  @param_bindings %{
    "got" => :got,
    "got-type" => :"got-type",
    "expected-type" => :"expected-type",
    "expected-length" => :"expected-length",
    "got-length" => :"got-length",
    "expected-count" => :"expected-count",
    "got-count" => :"got-count",
    "position" => :position,
    "choices" => :choices,
    "min" => :min,
    "max" => :max
  }

  defp feedback_bindings(params) do
    Map.new(params, fn {key, value} -> {Map.fetch!(@param_bindings, key), value} end)
  end

  @doc """
  Read-only bit display for resolved binary steps, with optional boundary
  separators and per-segment role colouring.
  """
  attr :bits, :string, required: true
  attr :boundary_every, :integer, default: 0
  attr :segments, :list, default: nil, doc: "list of {length, role} or nil"

  def bit_display(assigns) do
    assigns = assign(assigns, :cells, bit_display_cells(assigns))

    ~H"""
    <span class="bit-row">
      <%= for {char, index, role} <- @cells do %>
        <span
          :if={@boundary_every > 0 and index > 0 and rem(index, @boundary_every) == 0}
          class="bit-sep-mid"
        >
        </span><span class={["bit", role && "bit-#{role}"]}>{char}</span>
      <% end %>
    </span>
    """
  end

  defp bit_display_cells(%{bits: bits, segments: segments}) do
    chars = String.graphemes(bits)
    roles = segment_roles(segments, length(chars))

    chars
    |> Enum.with_index()
    |> Enum.map(fn {char, index} -> {char, index, Enum.at(roles, index)} end)
  end

  defp segment_roles(nil, length), do: List.duplicate(nil, length)

  defp segment_roles(segments, _length) do
    Enum.flat_map(segments, fn {count, role} ->
      List.duplicate(if(role == :plain, do: nil, else: role), count)
    end)
  end

  @doc "Display label of a format choice identifier (shared with the sandbox)."
  def format_choice_text(choice), do: AppWeb.SandboxComponents.format_choice_label(choice)

  @doc "Hex display of a byte: `C3` (no 0x prefix, exercise style)."
  def byte_text(byte) do
    byte |> Integer.to_string(16) |> String.pad_leading(2, "0")
  end

  @doc "U+XXXX label."
  def code_point_text(code_point) do
    "U+" <> String.pad_leading(Integer.to_string(code_point, 16), 4, "0")
  end
end
