defmodule AppWeb.SandboxLive.Utf8Encode do
  @moduledoc """
  UTF-8 encode sandbox: code point in, fully-revealed step decomposition out.
  The input lives in the `?input=` query param (shareable URLs); typing
  patches the URL (debounced) and `handle_params` recomputes everything.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.CodePointLabels
  alias Charset.Sandbox.InputParser

  @path "/sandbox/encode/utf-8"
  @default_input "U+00E9"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("UTF-8 sandbox (encode)"))}
  end

  @impl true
  def handle_params(params, _uri, socket) do
    raw = repair_plus(params["input"] || @default_input)
    {:noreply, socket |> assign(:raw_input, raw) |> assign_outcome(raw)}
  end

  @impl true
  def handle_event("input-changed", %{"input" => value}, socket) do
    query = URI.encode_query(%{"input" => value})
    path = localized_path(socket.assigns.locale, @path) <> "?" <> query
    {:noreply, push_patch(socket, to: path, replace: true)}
  end

  # `+` in query strings decodes as a literal space, so a hand-copied URL
  # like `?input=U+1F389` lands here as "U 1F389". Repair only the
  # start-of-string `U ` / `u ` pattern (the U+XXXX notation), not every
  # space - the parser trims surrounding whitespace, so a blanket replace
  # would corrupt those cases.
  defp repair_plus(input), do: Regex.replace(~r/^([Uu]) /, input, "\\1+")

  defp assign_outcome(socket, raw) do
    case InputParser.parse(raw) do
      {:ok, code_point} ->
        steps = Sandbox.encode_utf8(code_point)
        label = CodePointLabels.lookup(code_point)
        hex_bytes = Enum.find(steps, &match?(%Step.HexBytes{}, &1))

        assign(socket,
          error: nil,
          code_point: code_point,
          code_point_label: CodePoint.format(code_point),
          glyph: if(label == nil, do: <<code_point::utf8>>),
          label: label,
          format_step: Enum.find(steps, &match?(%Step.Format{}, &1)),
          binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
          bit_groups_step: Enum.find(steps, &match?(%Step.BitGroups{}, &1)),
          bytes: hex_bytes.expected
        )

      {:error, reason} ->
        assign(socket, error: error_message(reason), code_point: nil)
    end
  end

  defp error_message(:empty), do: gettext("Enter a code point.")
  defp error_message(:unparseable), do: gettext("Unrecognised format.")
  defp error_message(:out_of_range), do: gettext("Outside the Unicode range (0 → U+10FFFF).")

  defp error_message(:surrogate),
    do: gettext("Surrogates (U+D800 → U+DFFF) are not valid UTF-8.")

  defp format_desc(1),
    do: gettext("Range `U+0000` -> `U+007F` - 7 useful bits, plain ASCII form.")

  defp format_desc(2),
    do: gettext("Range `U+0080` -> `U+07FF` - 11 useful bits, beyond ASCII.")

  defp format_desc(3),
    do: gettext("Range `U+0800` -> `U+FFFF` - 16 useful bits, BMP beyond Latin extended.")

  defp format_desc(4),
    do:
      gettext(
        "Range `U+10000` -> `U+10FFFF` - 21 useful bits, supplementary planes (emojis, etc.)."
      )

  defp markers_desc(1),
    do:
      gettext(
        "The byte carries the marker `0` (1 bit). ASCII-compatible code point, no continuation bytes."
      )

  defp markers_desc(2),
    do:
      gettext(
        "The first byte carries the marker `110` (3 bits); the next carries `10` (continuation)."
      )

  defp markers_desc(3),
    do:
      gettext(
        "The first byte carries the marker `1110` (4 bits); the 2 following carry `10` (continuation)."
      )

  defp markers_desc(4),
    do:
      gettext(
        "The first byte carries the marker `11110` (5 bits); the 3 following carry `10` (continuation)."
      )

  defp hex_desc(1), do: gettext("Each binary byte becomes its hex value on `1` digit.")

  defp hex_desc(count),
    do: gettext("Each binary byte becomes its hex value on `%{count}` digits.", count: count)

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/encode/utf-8"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("UTF-8 sandbox (encode)")}
          subtitle={gettext("Convert a Unicode code point to UTF-8.")}
        />

        <.sandbox_input
          id="sb-cp"
          label={gettext("Code point")}
          value={@raw_input}
          placeholder={gettext("U+00E9, 0xE9, 233 or the character itself")}
          error={@error}
        >
          <:help>{gettext("Accepts U+XXXX, 0xXX, decimal, or a single character.")}</:help>
        </.sandbox_input>

        <section :if={@code_point} class="section-card mb-6">
          <.glyph_line
            code_point_label={@code_point_label}
            glyph={@glyph}
            label={@label}
            badge={byte_badge(length(@bytes))}
          />

          <div class="flex flex-col gap-3">
            <.result_row label={gettext("Decimal")}>
              <p class="hex text-xl font-medium">{@code_point}</p>
            </.result_row>
            <.result_row label={gettext("Hexadecimal")}>
              <p class="hex text-xl font-medium">{Enum.map_join(@bytes, " ", &hex_label/1)}</p>
            </.result_row>
            <.result_row label={gettext("Binary")}>
              <.utf8_bit_rows bytes={@bytes} />
            </.result_row>
          </div>
        </section>

        <%!-- Step-by-step timeline: numbered neutral dots, nothing to
             validate in the sandbox. --%>
        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step :if={@format_step} number={1} title={gettext("Pick the UTF-8 form")}>
              <:desc><.inline_desc text={format_desc(length(@bytes))} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {format_choice_label(@format_step.expected)}
                </span>
              </div>
            </.sandbox_step>

            <.sandbox_step :if={@binary_step} number={2} title={gettext("Convert to binary")}>
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Code point %{cp} fits in %{bits} significant bits (padded to the form's payload length).",
                    cp: @code_point_label,
                    bits: @binary_step.length
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step :if={@bit_groups_step} number={3} title={gettext("Split into chunks")}>
              <:desc>
                <.inline_desc text={
                  gettext("Split by the form's payload-slot widths (%{slots} bits).",
                    slots: Enum.map_join(@bit_groups_step.expected, " + ", &String.length/1)
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{Enum.join(@bit_groups_step.expected, " | ")}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              number={(@bit_groups_step && 4) || 3}
              title={gettext("Insert the markers")}
            >
              <:desc><.inline_desc text={markers_desc(length(@bytes))} /></:desc>
              <div class="surface px-5 py-4">
                <.utf8_bit_rows bytes={@bytes} labelled />
              </div>
            </.sandbox_step>

            <.sandbox_step
              number={(@bit_groups_step && 5) || 4}
              title={gettext("Convert to hexadecimal")}
              last
            >
              <:desc><.inline_desc text={hex_desc(length(@bytes))} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base font-medium">
                  {Enum.map_join(@bytes, " ", &hex_label/1)}
                </span>
              </div>
            </.sandbox_step>
          </ol>
        </section>
      </main>
    </Layouts.sandbox>
    """
  end
end
