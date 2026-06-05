defmodule AppWeb.SandboxComponents do
  @moduledoc """
  Function components shared by the sandbox LiveViews: page chrome, the
  step-by-step timeline, bit rows, and the InlineDesc mini-markup renderer.
  """

  use Phoenix.Component
  use Gettext, backend: AppWeb.Gettext

  alias Phoenix.HTML

  @doc """
  Page header: kicker, title, subtitle.
  """
  attr :title, :string, required: true
  attr :subtitle, :string, required: true

  def sandbox_header(assigns) do
    ~H"""
    <header class="mb-10">
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-3">
        sandbox
      </p>
      <h1 class="text-3xl font-medium leading-tight tracking-tight mb-2">
        {@title}
      </h1>
      <p class="text-sm text-mute leading-relaxed max-w-xl">
        {@subtitle}
      </p>
    </header>
    """
  end

  @doc """
  The sandbox input field: live-updating text input with error/help line.
  Emits the `input-changed` event (debounced) with an `input` param.
  """
  attr :id, :string, required: true
  attr :label, :string, required: true
  attr :value, :string, required: true
  attr :placeholder, :string, required: true
  attr :error, :string, default: nil

  slot :help, required: true

  def sandbox_input(assigns) do
    ~H"""
    <section class="surface-subtle p-5 sm:p-6 mb-8">
      <form class="field" phx-change="input-changed" onsubmit="return false">
        <label class="field-label" for={@id}>
          {@label}
        </label>
        <input
          id={@id}
          name="input"
          value={@value}
          class={["field-input field-input-mono", @error && "is-error"]}
          autocomplete="off"
          spellcheck="false"
          placeholder={@placeholder}
          phx-debounce="250"
        />
        <p :if={@error} class="field-error">{@error}</p>
        <p :if={!@error} class="field-help">{render_slot(@help)}</p>
      </form>
    </section>
    """
  end

  @doc """
  The glyph/label line of the result card: `U+00E9 é` or, for invisible code
  points, `U+0007 BEL (Bell)`.
  """
  attr :code_point_label, :string, required: true
  attr :glyph, :string, default: nil
  attr :label, :string, default: nil
  attr :badge, :string, required: true

  def glyph_line(assigns) do
    ~H"""
    <div class="flex items-baseline justify-between mb-4 gap-3 flex-wrap">
      <div class="flex items-baseline gap-3 flex-wrap">
        <span class="codepoint-glyph codepoint-label-lg">
          {@code_point_label}
        </span>
        <span :if={@glyph} class="codepoint-label-lg text-mute">
          {@glyph}
        </span>
        <span :if={!@glyph && @label} class="text-mute flex items-baseline gap-2 flex-wrap">
          <span class="font-mono font-medium text-xl">{@label}</span>
          <span class="text-xs text-faint">({label_description(@label)})</span>
        </span>
      </div>
      <span class="font-mono text-xs uppercase tracking-widest text-faint">
        {@badge}
      </span>
    </div>
    """
  end

  @doc """
  One labelled value block of the result card (decimal / hex / binary rows).
  """
  attr :label, :string, required: true

  slot :inner_block, required: true

  def result_row(assigns) do
    ~H"""
    <div>
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
        {@label}
      </p>
      {render_slot(@inner_block)}
    </div>
    """
  end

  @doc """
  One item of the step-by-step timeline: numbered neutral dot (nothing to
  validate in the sandbox), title, description, content.
  """
  attr :number, :integer, required: true
  attr :title, :string, required: true
  attr :last, :boolean, default: false

  slot :desc, required: true
  slot :inner_block, required: true

  def sandbox_step(assigns) do
    ~H"""
    <li class="flex gap-5">
      <div class="flex flex-col items-center">
        <span class="step-dot">{String.pad_leading(Integer.to_string(@number), 2, "0")}</span>
        <span :if={!@last} class="step-connector"></span>
      </div>
      <div class="flex-1 pb-6">
        <h3 class="text-sm font-medium mb-2 mt-1">{@title}</h3>
        <p class="text-sm text-mute mb-3 leading-relaxed">{render_slot(@desc)}</p>
        {render_slot(@inner_block)}
      </div>
    </li>
    """
  end

  @doc """
  The binary rows of a UTF-8 byte sequence, coloured by role: leader marker,
  continuation marker, payload. Optionally prefixed with a "byte N" label.
  """
  attr :bytes, :list, required: true, doc: "list of byte integers"
  attr :labelled, :boolean, default: false

  def utf8_bit_rows(assigns) do
    ~H"""
    <div class={["flex flex-col", (@labelled && "gap-2") || "gap-1.5"]}>
      <div
        :for={{byte, byte_index} <- Enum.with_index(@bytes)}
        class={[(@labelled && "flex items-center gap-3 flex-wrap") || "bit-row bit-row-tight"]}
      >
        <span :if={@labelled} class="font-mono text-xs text-faint min-w-[3.5rem]">
          byte {byte_index + 1}
        </span>
        <div :if={@labelled} class="bit-row bit-row-tight">
          <.utf8_bits byte={byte} byte_index={byte_index} byte_count={length(@bytes)} />
        </div>
        <.utf8_bits :if={!@labelled} byte={byte} byte_index={byte_index} byte_count={length(@bytes)} />
      </div>
    </div>
    """
  end

  attr :byte, :integer, required: true
  attr :byte_index, :integer, required: true
  attr :byte_count, :integer, required: true

  defp utf8_bits(assigns) do
    ~H"""
    <span
      :for={{bit, bit_index} <- Enum.with_index(bits(@byte))}
      class={["bit bit-sm", utf8_bit_class(@byte_index, bit_index, @byte_count)]}
    >
      {bit}
    </span>
    """
  end

  @doc """
  Bit colouring role for UTF-8 byte sequences.

  Continuation bytes: `10` is the full continuation marker (2 bits). Leader
  byte: the marker is the complete pattern, terminator included (`0` for
  1 byte, `110`, `1110`, `11110`) - the trailing `0` is what distinguishes
  e.g. `110` (2-byte) from `111` (3+ byte), it belongs to the marker, not to
  the payload.
  """
  def utf8_bit_class(byte_index, bit_index, _byte_count) when byte_index > 0 do
    if bit_index < 2, do: "bit-cont", else: "bit-payload"
  end

  def utf8_bit_class(0, bit_index, byte_count) do
    marker_length = if byte_count == 1, do: 1, else: byte_count + 1
    if bit_index < marker_length, do: "bit-marker", else: "bit-payload"
  end

  @doc "The 8 bits of a byte, as strings."
  def bits(byte) do
    byte |> Integer.to_string(2) |> String.pad_leading(8, "0") |> String.graphemes()
  end

  @doc ~S(Formats a byte as `0xC3`.)
  def hex_label(byte) do
    "0x" <> String.pad_leading(Integer.to_string(byte, 16), 2, "0")
  end

  @doc "Pluralized byte count badge."
  def byte_badge(n), do: ngettext("%{n} byte", "%{n} bytes", n, n: n)

  @doc """
  Display label of a `FormatChoice` identifier (closed set, translated).
  """
  def format_choice_label("format-choice.byte-count.1"), do: gettext("1 byte · U+0000 → U+007F")
  def format_choice_label("format-choice.byte-count.2"), do: gettext("2 bytes · U+0080 → U+07FF")
  def format_choice_label("format-choice.byte-count.3"), do: gettext("3 bytes · U+0800 → U+FFFF")

  def format_choice_label("format-choice.byte-count.4"),
    do: gettext("4 bytes · U+10000 → U+10FFFF")

  def format_choice_label("format-choice.code-unit.1"), do: gettext("1 code unit · BMP")

  def format_choice_label("format-choice.code-unit.2"),
    do: gettext("2 code units · surrogate pair")

  # Long-form expansion shown next to the short mnemonic label (e.g. `PUA` →
  # `Private Use Area`). The catalog lives in the "labels" gettext domain
  # (closed list maintained by hand, mirroring CodePointLabels); falls back to
  # the generic "non-printable" suffix when no expansion is registered, so the
  # UI stays informative if a new mnemonic lands without its translation.
  defp label_description(label) do
    case Gettext.dgettext(AppWeb.Gettext, "labels", label) do
      ^label -> gettext("non-printable")
      description -> description
    end
  end

  @doc """
  Renders a short i18n description with three light Markdown-ish features:

    * backtick spans become inline `<code>` (e.g. `0xD800`)
    * `[text^title]` becomes `<abbr title="title">text</abbr>` - `^` as
      separator because `{`/`}` carry interpolation meaning in the catalogs
    * newlines become `<br>` so multi-line strings stay readable

  Keeps the gettext catalogs free of HTML (writers use a tiny ASCII syntax).
  """
  attr :text, :string, required: true

  def inline_desc(assigns) do
    ~H"""
    <span>{render_tokens(@text)}</span>
    """
  end

  @inline_pattern ~r/\[([^\]^]+)\^([^\]]+)\]|`([^`]+)`/

  defp render_tokens(text) do
    text
    |> String.split("\n")
    |> Enum.map(&tokenize_line/1)
    |> Enum.intersperse([HTML.raw("<br/>")])
    |> Enum.concat()
  end

  defp tokenize_line(line) do
    @inline_pattern
    |> Regex.split(line, include_captures: true)
    |> Enum.map(fn part ->
      case Regex.run(@inline_pattern, part, capture: :all_but_first) do
        [abbr, title] ->
          HTML.raw(["<abbr title=\"", escape(title), "\">", escape(abbr), "</abbr>"])

        ["", "", code] ->
          HTML.raw(["<code class=\"inline-code\">", escape(code), "</code>"])

        nil ->
          part
      end
    end)
  end

  defp escape(text), do: text |> HTML.html_escape() |> HTML.safe_to_string()
end
