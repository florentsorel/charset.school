defmodule AppWeb.SandboxLive.Utf32Decode do
  @moduledoc """
  UTF-32 decode sandbox: 4 bytes + given endianness in, the code point out.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.BytesParser
  alias Charset.Sandbox.CodePointLabels

  @path "/sandbox/decode/utf-32"
  # The little-endian encoding of U+1F389, so the page lands on a
  # supplementary-plane example on first load.
  @default_input "89 F3 01 00"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("UTF-32 sandbox (decode)"))}
  end

  @impl true
  def handle_params(params, _uri, socket) do
    raw = params["bytes"] || @default_input
    endian = if params["endian"] == "big", do: :big, else: :little
    {:noreply, socket |> assign(raw_input: raw, endian: endian) |> assign_outcome(raw, endian)}
  end

  @impl true
  def handle_event("input-changed", %{"input" => value, "endian" => endian}, socket) do
    query = URI.encode_query(%{"bytes" => value, "endian" => endian})
    path = localized_path(socket.assigns.locale, @path) <> "?" <> query
    {:noreply, push_patch(socket, to: path, replace: true)}
  end

  defp assign_outcome(socket, raw, endian) do
    encoding = if endian == :big, do: :utf32be, else: :utf32le

    with {:ok, bytes} <- BytesParser.parse(raw),
         {:ok, code_point} <- decode(bytes, encoding) do
      steps = Sandbox.decode_utf32(bytes, code_point, endian)
      label = CodePointLabels.lookup(code_point)

      assign(socket,
        error: nil,
        bytes: :binary.bin_to_list(bytes),
        code_point: code_point,
        code_point_label: CodePoint.format(code_point),
        glyph: if(label == nil, do: <<code_point::utf8>>),
        label: label,
        endian_step: Enum.find(steps, &match?(%Step.Endianness{}, &1)),
        binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
        code_point_step: Enum.find(steps, &match?(%Step.CodePointEntry{}, &1))
      )
    else
      {:error, reason} ->
        assign(socket, error: error_message(reason), code_point: nil)
    end
  end

  defp decode(bytes, encoding) do
    case Codec.decode(bytes, encoding) do
      {:ok, code_point} -> {:ok, code_point}
      {:error, _decode_error} -> {:error, :decoder}
    end
  end

  defp error_message(:empty), do: gettext("Enter a hex byte sequence.")
  defp error_message(:invalid_hex), do: gettext("Non-hex characters detected.")

  defp error_message(:odd_length),
    do: gettext("Last byte is incomplete (odd number of hex digits).")

  defp error_message(:too_long),
    do: gettext("Too many bytes: UTF-32 encodes a single code point in exactly 4 bytes.")

  defp error_message(:decoder),
    do:
      gettext(
        "This sequence doesn't form a valid UTF-32 code point. It needs exactly 4 bytes, and the value must be in `U+0000` → `U+10FFFF` excluding surrogates."
      )

  defp endianness_desc(:big),
    do: gettext("The 4 bytes are read high-order byte first (`Big Endian`).")

  defp endianness_desc(:little),
    do:
      gettext(
        "The 4 bytes are read low-order byte first (`Little Endian`). Mentally reverse the byte order before assembling the number."
      )

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/decode/utf-32"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("UTF-32 sandbox (decode)")}
          subtitle={
            gettext("Convert 4 UTF-32 bytes into a Unicode code point, given the endianness.")
          }
        />

        <.sandbox_input
          id="sb-bytes"
          label={gettext("Bytes")}
          value={@raw_input}
          placeholder={gettext("00 00 00 E9, 89 F3 01 00, or 000000E9")}
          error={@error}
        >
          <:help>
            <.inline_desc text={
              gettext("Hex (with or without `0x`). UTF-32 takes exactly 4 bytes per code point.")
            } />
          </:help>
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
            <.result_row label={gettext("Bytes")}>
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
            <.sandbox_step :if={@endian_step} number={1} title={gettext("Given endianness")}>
              <:desc><.inline_desc text={endianness_desc(@endian_step.expected)} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {endian_label(@endian_step.expected)}
                </span>
              </div>
            </.sandbox_step>

            <.sandbox_step :if={@binary_step} number={2} title={gettext("Reassemble the binary")}>
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Concatenate the 4 bytes (reordered as Big Endian) to reform the full 32-bit binary.\nIn UTF-32 this binary IS the code point - no marker to strip, no surrogate to recombine."
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@code_point_step}
              number={3}
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
