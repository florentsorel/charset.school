defmodule AppWeb.SandboxLive.Utf32Encode do
  @moduledoc """
  UTF-32 encode sandbox: the simplest UTF flow - the code point IS the
  32-bit value, only the byte order varies.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.CodePointLabels
  alias Charset.Sandbox.InputParser

  @path "/sandbox/encode/utf-32"
  @default_input "U+1F389"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("UTF-32 sandbox (encode)"))}
  end

  @impl true
  def handle_params(params, _uri, socket) do
    raw = repair_plus(params["input"] || @default_input)
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
        steps = Sandbox.encode_utf32(code_point, endian)
        label = CodePointLabels.lookup(code_point)
        hex_bytes = Enum.find(steps, &match?(%Step.HexBytes{}, &1))

        assign(socket,
          error: nil,
          code_point: code_point,
          code_point_label: CodePoint.format(code_point),
          glyph: if(label == nil, do: <<code_point::utf8>>),
          label: label,
          endian_step: Enum.find(steps, &match?(%Step.Endianness{}, &1)),
          binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
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
    do: gettext("Surrogates (U+D800 → U+DFFF) are not valid Unicode scalar values.")

  defp endianness_desc(:big),
    do:
      gettext(
        "The high-order byte comes first (`Big Endian`). The 4 bytes are written in the natural order of the number."
      )

  defp endianness_desc(:little),
    do:
      gettext(
        "The low-order byte comes first (`Little Endian`). The 4 bytes are written in reverse order from the number."
      )

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/encode/utf-32"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("UTF-32 sandbox (encode)")}
          subtitle={
            gettext("Convert a Unicode code point into 4 UTF-32 bytes, with a chosen endianness.")
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

            <.sandbox_step :if={@binary_step} number={2} title={gettext("Convert to 32-bit binary")}>
              <:desc>
                <.inline_desc text={
                  gettext(
                    "UTF-32 always uses 32 bits, regardless of the code point.\nCode point `%{cp}` fits in at most 21 significant bits (Unicode caps at `U+10FFFF`), so the 11 high bits are always zero.\nThis padding is what makes UTF-32 simple: no format to guess, no marker, just the code point in its 32 bits.",
                    cp: @code_point_label
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step number={3} title={gettext("Convert to hexadecimal")} last>
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Split the binary into 4 packets of 8 bits, then write each packet in hex.\nThe order of the 4 bytes depends on the chosen endianness."
                  )
                } />
              </:desc>
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
