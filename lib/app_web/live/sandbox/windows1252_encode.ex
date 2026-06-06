defmodule AppWeb.SandboxLive.Windows1252Encode do
  @moduledoc """
  Windows-1252 encode sandbox. Highlights which CP1252 range the byte
  belongs to, so the pedagogical goal (the Microsoft extension at
  0x80..0x9F) is visible at a glance.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.CodePointLabels
  alias Charset.Sandbox.InputParser

  @path "/sandbox/encode/windows-1252"
  # U+20AC (Euro) - the canonical Microsoft extension at byte 0x80, perfect
  # to demonstrate what makes CP1252 different from plain Latin-1.
  @default_input "U+20AC"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("Windows-1252 sandbox (encode)"))}
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

  defp repair_plus(input), do: Regex.replace(~r/^([Uu]) /, input, "\\1+")

  defp assign_outcome(socket, raw) do
    with {:ok, code_point} <- InputParser.parse(raw),
         {:ok, <<byte>>} <- encode(code_point) do
      steps = Sandbox.encode_windows1252(code_point)
      label = CodePointLabels.lookup(code_point)

      assign(socket,
        error: nil,
        code_point: code_point,
        code_point_label: CodePoint.format(code_point),
        glyph: if(label == nil, do: <<code_point::utf8>>),
        label: label,
        binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
        bytes: [byte],
        byte_range: byte_range(byte)
      )
    else
      {:error, reason} ->
        assign(socket, error: error_message(reason), code_point: nil)
    end
  end

  defp encode(code_point) do
    case Codec.encode(code_point, :windows1252) do
      {:ok, bytes} -> {:ok, bytes}
      {:error, _encode_error} -> {:error, :not_encodable}
    end
  end

  defp byte_range(byte) when byte <= 0x7F, do: :ascii
  defp byte_range(byte) when byte <= 0x9F, do: :special
  defp byte_range(_byte), do: :latin1

  defp error_message(:empty), do: gettext("Enter a code point.")
  defp error_message(:unparseable), do: gettext("Unrecognised format.")
  defp error_message(:out_of_range), do: gettext("Outside the Unicode range (0 → U+10FFFF).")

  defp error_message(:surrogate),
    do: gettext("Surrogates (U+D800 → U+DFFF) are not valid Unicode scalar values.")

  defp error_message(:not_encodable),
    do:
      gettext(
        "This code point isn't in the Windows-1252 table. Outside the ASCII and Latin-1 ranges, only 27 specific characters can be encoded."
      )

  defp range_desc(:ascii),
    do: gettext("Range `0x00` → `0x7F` - identical to ASCII (and to Latin-1).")

  defp range_desc(:special),
    do:
      gettext(
        "Range `0x80` → `0x9F` - the Microsoft extension: 27 printable characters like `€`, `™`, `—`, smart quotes, ... This is where [CP1252^Code Page 1252] differs from Latin-1 (which has C1 controls in that slot). 5 bytes remain unassigned: `0x81`, `0x8D`, `0x8F`, `0x90`, `0x9D`."
      )

  defp range_desc(:latin1),
    do: gettext("Range `0xA0` → `0xFF` - identical to Latin-1 (accented characters, symbols).")

  defp binary_desc(:ascii, cp_label),
    do:
      gettext(
        "Code point `%{cp}` is in the ASCII range: its byte `0x00` → `0x7F` reads straight into binary (8 bits, high bit `0`).",
        cp: cp_label
      )

  defp binary_desc(:special, cp_label),
    do:
      gettext(
        "Code point `%{cp}` isn't in an identity range. Look up the [CP1252^Code Page 1252] table to find its byte between `0x80` and `0x9F`, then convert to binary.",
        cp: cp_label
      )

  defp binary_desc(:latin1, cp_label),
    do:
      gettext(
        "Code point `%{cp}` is in the Latin-1 Supplement range (`U+00A0` → `U+00FF`): its byte matches the code point value, written as 8 bits.",
        cp: cp_label
      )

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/encode/windows-1252"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("Windows-1252 sandbox (encode)")}
          subtitle={gettext("Convert a Unicode code point into a single Windows-1252 (CP1252) byte.")}
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
            <.result_row :if={@binary_step} label={gettext("Binary")}>
              <.plain_bit_rows bytes={@bytes} />
            </.result_row>
            <div :if={@byte_range}>
              <p class="font-mono text-xs uppercase tracking-widest text-faint mb-1.5">
                <.inline_desc text={gettext("[CP1252^Code Page 1252] range")} />
              </p>
              <p class="text-sm text-mute">
                <.inline_desc text={range_desc(@byte_range)} />
              </p>
            </div>
          </div>
        </section>

        <%!-- Simpler timeline than UTF-8/16: no markers, no surrogate split
             - just binary and hex. --%>
        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step
              :if={@binary_step}
              number={1}
              title={gettext("Read the byte value in binary")}
            >
              <:desc>
                <.inline_desc text={binary_desc(@byte_range, @code_point_label)} />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step number={2} title={gettext("Convert to hexadecimal")} last>
              <:desc>
                <.inline_desc text={gettext("The binary byte becomes its 2-digit hex value.")} />
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
