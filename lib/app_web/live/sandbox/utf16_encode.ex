defmodule AppWeb.SandboxLive.Utf16Encode do
  @moduledoc """
  UTF-16 encode sandbox: code point + chosen endianness in, revealed step
  decomposition out (endianness, code-unit form, binary, surrogate split,
  hex bytes).
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.CodePointLabels
  alias Charset.Sandbox.InputParser

  @path "/sandbox/encode/utf-16"
  @default_input "U+1F389"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("UTF-16 sandbox (encode)"))}
  end

  @impl true
  def handle_params(params, _uri, socket) do
    raw = repair_plus(params["input"] || @default_input)
    # Little-endian by default: it matches what users encounter most in
    # practice (Windows internals, modern x86/ARM in-memory). BE is the
    # RFC 2781 default but more relevant for network protocols.
    endian = if params["endian"] == "big", do: :big, else: :little
    {:noreply, socket |> assign(raw_input: raw, endian: endian) |> assign_outcome(raw, endian)}
  end

  @impl true
  def handle_event("input-changed", %{"input" => value, "endian" => endian}, socket) do
    query = URI.encode_query(%{"input" => value, "endian" => endian})
    path = localized_path(socket.assigns.locale, @path) <> "?" <> query
    {:noreply, push_patch(socket, to: path, replace: true)}
  end

  defp repair_plus(input), do: Regex.replace(~r/^([Uu]) /, input, "\\1+")

  defp assign_outcome(socket, raw, endian) do
    case InputParser.parse(raw) do
      {:ok, code_point} ->
        steps = Sandbox.encode_utf16(code_point, endian)
        label = CodePointLabels.lookup(code_point)
        hex_bytes = Enum.find(steps, &match?(%Step.HexBytes{}, &1))

        assign(socket,
          error: nil,
          code_point: code_point,
          code_point_label: CodePoint.format(code_point),
          glyph: if(label == nil, do: <<code_point::utf8>>),
          label: label,
          endian_step: Enum.find(steps, &match?(%Step.Endianness{}, &1)),
          format_step: Enum.find(steps, &match?(%Step.Format{}, &1)),
          binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
          bit_groups_step: Enum.find(steps, &match?(%Step.BitGroups{}, &1)),
          bytes: hex_bytes.expected,
          code_unit_count: div(length(hex_bytes.expected), 2)
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

  defp endianness_desc(:big),
    do: gettext("The high-order byte of each code unit comes first (`Big Endian`).")

  defp endianness_desc(:little),
    do: gettext("The low-order byte of each code unit comes first (`Little Endian`).")

  defp format_desc(1),
    do:
      gettext(
        "Range `U+0000` → `U+FFFF` - the [BMP^Basic Multilingual Plane], which covers most of the world's scripts. Fits in a single 16-bit code unit, 2 bytes."
      )

  defp format_desc(2),
    do:
      gettext(
        "Range `U+10000` → `U+10FFFF` - beyond the [BMP^Basic Multilingual Plane], in the supplementary planes (emojis, historic scripts, rare [CJK^Chinese, Japanese, Korean]...). Encoded as a surrogate pair: 2 code units = 4 bytes."
      )

  defp binary_desc(1, cp_label),
    do: gettext("Code point `%{cp}` fits in 16 significant bits.", cp: cp_label)

  defp binary_desc(2, _cp_label),
    do: gettext("Subtract `0x10000` and keep the remaining 20 bits.")

  defp hex_desc(1), do: gettext("The single code unit yields 2 bytes, ordered by endianness.")

  defp hex_desc(2),
    do:
      gettext(
        "The 2 code units (high + low surrogate) yield 4 bytes, each code unit ordered by endianness."
      )

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/encode/utf-16"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("UTF-16 sandbox (encode)")}
          subtitle={
            gettext("Convert a Unicode code point into UTF-16 bytes, with a chosen endianness.")
          }
        />

        <.sandbox_input
          id="sb-cp"
          label={gettext("Code point")}
          value={@raw_input}
          placeholder={gettext("U+00E9, 0xE9, 233 or the character itself")}
          error={@error}
        >
          <:help>{gettext("Accepts U+XXXX, 0xXX, decimal, or a single character.")}</:help>
          <:extra><.endian_radios endian={@endian} /></:extra>
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
              <.plain_bit_rows bytes={@bytes} />
            </.result_row>
          </div>
        </section>

        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step :if={@endian_step} number={1} title={gettext("Pick the endianness")}>
              <:desc><.inline_desc text={endianness_desc(@endian_step.expected)} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {endian_label(@endian_step.expected)}
                </span>
              </div>
            </.sandbox_step>

            <.sandbox_step :if={@format_step} number={2} title={gettext("Pick the UTF-16 form")}>
              <:desc><.inline_desc text={format_desc(@code_unit_count)} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {format_choice_label(@format_step.expected)}
                </span>
              </div>
            </.sandbox_step>

            <.sandbox_step :if={@binary_step} number={3} title={gettext("Convert to binary")}>
              <:desc><.inline_desc text={binary_desc(@code_unit_count, @code_point_label)} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@bit_groups_step}
              number={4}
              title={gettext("Split for the surrogates")}
            >
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Split the 20 bits into 2 packets of 10 bits.\nThe left 10 bits (high-order) form the high surrogate, the right 10 bits (low-order) form the low surrogate.\nEach 10-bit packet represents an integer between 0 and 1,023 (10 bits = 2¹⁰ = 1,024 values). Add it to the surrogate base: `0xD800` for the high, `0xDC00` for the low."
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{Enum.join(@bit_groups_step.expected, " | ")}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              number={(@bit_groups_step && 5) || 4}
              title={gettext("Convert to hexadecimal")}
              last
            >
              <:desc><.inline_desc text={hex_desc(@code_unit_count)} /></:desc>
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
