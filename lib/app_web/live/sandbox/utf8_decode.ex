defmodule AppWeb.SandboxLive.Utf8Decode do
  @moduledoc """
  UTF-8 decode sandbox: bytes in, fully-revealed step decomposition out. The
  input lives in the `?bytes=` query param. No `+` repair needed here: hex
  byte input cannot contain a literal `+`.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.BytesParser
  alias Charset.Sandbox.CodePointLabels

  @path "/sandbox/decode/utf-8"
  @default_input "C3 A9"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("UTF-8 sandbox (decode)"))}
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
      steps = Sandbox.decode_utf8(bytes, code_point)
      label = CodePointLabels.lookup(code_point)

      assign(socket,
        error: nil,
        bytes: :binary.bin_to_list(bytes),
        code_point: code_point,
        code_point_label: CodePoint.format(code_point),
        glyph: if(label == nil, do: <<code_point::utf8>>),
        label: label,
        format_step: Enum.find(steps, &match?(%Step.Format{}, &1)),
        binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
        bit_groups_step: Enum.find(steps, &match?(%Step.BitGroups{}, &1)),
        code_point_step: Enum.find(steps, &match?(%Step.CodePointEntry{}, &1))
      )
    else
      {:error, reason} ->
        assign(socket, error: error_message(reason), code_point: nil)
    end
  end

  defp decode(bytes) do
    case Codec.decode(bytes, :utf8) do
      {:ok, code_point} -> {:ok, code_point}
      {:error, _decode_error} -> {:error, :decoder}
    end
  end

  defp error_message(:empty), do: gettext("Enter a hex byte sequence.")
  defp error_message(:invalid_hex), do: gettext("Non-hex characters detected.")

  defp error_message(:odd_length),
    do: gettext("Last byte is incomplete (odd number of hex digits).")

  defp error_message(:too_long),
    do: gettext("Too many bytes: UTF-8 encodes a single code point in at most 4 bytes.")

  defp error_message(:decoder), do: gettext("This byte sequence is not valid UTF-8.")

  defp format_desc(1),
    do: gettext("First byte `0xxxxxxx` (high bit `0`) - plain ASCII, 1 byte only.")

  defp format_desc(2),
    do: gettext("First byte `110xxxxx` (two `1`s then `0`) - 2-byte form.")

  defp format_desc(3),
    do: gettext("First byte `1110xxxx` (three `1`s then `0`) - 3-byte form.")

  defp format_desc(4),
    do: gettext("First byte `11110xxx` (four `1`s then `0`) - 4-byte form.")

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/decode/utf-8"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("UTF-8 sandbox (decode)")}
          subtitle={gettext("Convert a sequence of UTF-8 bytes into a Unicode code point.")}
        />

        <.sandbox_input
          id="sb-bytes"
          label={gettext("Bytes")}
          value={@raw_input}
          placeholder={gettext("C3 A9, 0xC3 0xA9, or C3A9")}
          error={@error}
        >
          <:help>
            <.inline_desc text={
              gettext("Hex (with or without `0x`), separated by spaces / commas / no separator.")
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
            <.result_row label={gettext("Bytes")}>
              <p class="hex text-xl font-medium">{Enum.map_join(@bytes, " ", &hex_label/1)}</p>
            </.result_row>
            <.result_row label={gettext("Binary")}>
              <.utf8_bit_rows bytes={@bytes} />
            </.result_row>
          </div>
        </section>

        <%!-- Timeline mirrors the encode page but the flow is reversed:
             identify byte count -> strip markers -> assemble bits -> read
             as code point. --%>
        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step :if={@format_step} number={1} title={gettext("Identify the byte count")}>
              <:desc><.inline_desc text={format_desc(length(@bytes))} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {format_choice_label(@format_step.expected)}
                </span>
              </div>
            </.sandbox_step>

            <.sandbox_step :if={@bit_groups_step} number={2} title={gettext("Extract the data bits")}>
              <:desc>
                <.inline_desc text={
                  gettext(
                    "For each byte, strip the format marker (`110`/`1110`/`11110` on the leader, `10` on continuations) - what remains are the data bits."
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{Enum.join(@bit_groups_step.expected, " | ")}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@binary_step}
              number={(@bit_groups_step && 3) || 2}
              title={gettext("Reassemble the binary")}
            >
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Concatenate the groups to rebuild the code point's binary (%{bits} significant bits).",
                    bits: @binary_step.length
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@code_point_step}
              number={(@bit_groups_step && 4) || 3}
              title={gettext("Convert to a code point")}
              last
            >
              <:desc>
                <.inline_desc text={
                  gettext(
                    "The binary equals `%{decimal}` in decimal, i.e. `%{cp}` in Unicode notation.",
                    decimal: @code_point_step.expected,
                    cp: @code_point_label
                  )
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
