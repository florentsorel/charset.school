defmodule AppWeb.SandboxLive.Windows1252Decode do
  @moduledoc """
  Windows-1252 decode sandbox: one byte in, its code point out, with the
  CP1252 range highlighted (the Microsoft extension being the classic trap).
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.BytesParser
  alias Charset.Sandbox.CodePointLabels

  @path "/sandbox/decode/windows-1252"
  # 0x80 (Euro) - the Microsoft extension that nobody expects when used to
  # plain Latin-1.
  @default_input "80"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("Windows-1252 sandbox (decode)"))}
  end

  @impl true
  def handle_params(params, _uri, socket) do
    raw = params["bytes"] || @default_input
    {:noreply, socket |> assign(:raw_input, raw) |> assign_outcome(raw)}
  end

  @impl true
  def handle_event("input-changed", %{"input" => value}, socket) do
    query = URI.encode_query(%{"bytes" => value})
    path = localized_path(socket.assigns.locale, @path) <> "?" <> query
    {:noreply, push_patch(socket, to: path, replace: true)}
  end

  defp assign_outcome(socket, raw) do
    with {:ok, bytes} <- BytesParser.parse(raw),
         {:ok, code_point} <- decode(bytes) do
      steps = Sandbox.decode_windows1252(bytes, code_point)
      label = CodePointLabels.lookup(code_point)
      <<byte>> = bytes

      assign(socket,
        error: nil,
        bytes: [byte],
        byte_range: byte_range(byte),
        code_point: code_point,
        code_point_label: CodePoint.format(code_point),
        glyph: if(label == nil, do: <<code_point::utf8>>),
        label: label,
        binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
        code_point_step: Enum.find(steps, &match?(%Step.CodePointEntry{}, &1))
      )
    else
      {:error, reason} ->
        assign(socket, error: error_message(reason), code_point: nil)
    end
  end

  defp decode(bytes) do
    case Codec.decode(bytes, :windows1252) do
      {:ok, code_point} -> {:ok, code_point}
      {:error, _decode_error} -> {:error, :not_decodable}
    end
  end

  defp byte_range(byte) when byte <= 0x7F, do: :ascii
  defp byte_range(byte) when byte <= 0x9F, do: :special
  defp byte_range(_byte), do: :latin1

  defp error_message(:empty), do: gettext("Enter a hex byte.")
  defp error_message(:invalid_hex), do: gettext("Non-hex characters detected.")

  defp error_message(:odd_length),
    do: gettext("The byte is incomplete (odd number of hex digits).")

  defp error_message(:too_long),
    do: gettext("Too many bytes: Windows-1252 encodes a single character in one byte.")

  defp error_message(:not_decodable),
    do:
      gettext(
        "This byte doesn't map to any character in Windows-1252. Either it's an unassigned byte (`0x81`, `0x8D`, `0x8F`, `0x90`, `0x9D`), or you entered more than one byte (CP1252 = 1 byte per character)."
      )

  defp range_desc(:ascii),
    do: gettext("Range `0x00` → `0x7F` - ASCII identity: the byte is its own code point.")

  defp range_desc(:special),
    do:
      gettext(
        "Range `0x80` → `0x9F` - the Microsoft extension. This is where Windows-1252 diverges from Latin-1: 27 printable characters (`€` at `0x80`, `™` at `0x99`, em-dash `—` at `0x97`, smart quotes, ...) instead of C1 controls."
      )

  defp range_desc(:latin1),
    do: gettext("Range `0xA0` → `0xFF` - Latin-1 identity: the byte is its own code point.")

  defp code_point_desc(:ascii, decimal, cp_label),
    do:
      gettext(
        "The byte is `%{decimal}` in decimal, identical to its code point (ASCII range: pure identity). That's `%{cp}` in Unicode notation.",
        decimal: decimal,
        cp: cp_label
      )

  defp code_point_desc(:special, decimal, cp_label),
    do:
      gettext(
        "The byte is in the Microsoft extension. The [CP1252^Code Page 1252] table maps it to `%{cp}` (decimal `%{decimal}`). Classic trap: if you read this same byte as Latin-1, you get an invisible C1 control instead of a printable character.",
        decimal: decimal,
        cp: cp_label
      )

  defp code_point_desc(:latin1, decimal, cp_label),
    do:
      gettext(
        "The byte is `%{decimal}` in decimal, identical to its code point (Latin-1 range: pure identity). That's `%{cp}` in Unicode notation.",
        decimal: decimal,
        cp: cp_label
      )

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/decode/windows-1252"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("Windows-1252 sandbox (decode)")}
          subtitle={gettext("Convert a Windows-1252 (CP1252) byte into a Unicode code point.")}
        />

        <.sandbox_input
          id="sb-bytes"
          label={gettext("Byte")}
          value={@raw_input}
          placeholder={gettext("80, 0x80, or E9")}
          error={@error}
        >
          <:help>
            <.inline_desc text={
              gettext("Hex (with or without `0x`). Windows-1252 encodes one character per byte.")
            } />
          </:help>
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
            <.result_row label={gettext("Byte")}>
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

        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step :if={@binary_step} number={1} title={gettext("Read the value in binary")}>
              <:desc>
                <.inline_desc text={gettext("Write the byte value as 8 bits.")} />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@code_point_step}
              number={2}
              title={gettext("Convert to a code point")}
              last
            >
              <:desc>
                <.inline_desc text={
                  code_point_desc(@byte_range, @code_point_step.expected, @code_point_label)
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base font-medium">{@code_point_label}</span>
              </div>
            </.sandbox_step>
          </ol>
        </section>
      </main>
    </Layouts.sandbox>
    """
  end
end
