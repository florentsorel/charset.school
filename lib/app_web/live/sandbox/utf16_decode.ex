defmodule AppWeb.SandboxLive.Utf16Decode do
  @moduledoc """
  UTF-16 decode sandbox: bytes + given endianness in, revealed step
  decomposition out.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.BytesParser
  alias Charset.Sandbox.CodePointLabels

  @path "/sandbox/decode/utf-16"
  # The little-endian encoding of U+1F389, so the page shows a complete
  # surrogate-pair flow on first load.
  @default_input "3C D8 89 DF"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("UTF-16 sandbox (decode)"))}
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
    encoding = if endian == :big, do: :utf16be, else: :utf16le

    with {:ok, bytes} <- BytesParser.parse(raw),
         {:ok, code_point} <- decode(bytes, encoding) do
      steps = Sandbox.decode_utf16(bytes, code_point, endian)
      label = CodePointLabels.lookup(code_point)

      assign(socket,
        error: nil,
        bytes: :binary.bin_to_list(bytes),
        code_point: code_point,
        code_point_label: CodePoint.format(code_point),
        glyph: if(label == nil, do: <<code_point::utf8>>),
        label: label,
        endian_step: Enum.find(steps, &match?(%Step.Endianness{}, &1)),
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
    do: gettext("Too many bytes: UTF-16 encodes a single code point in 2 or 4 bytes.")

  defp error_message(:decoder),
    do:
      gettext(
        "This sequence doesn't form a single UTF-16 code point. The same byte stream can be one valid code point in BE and multiple in LE (or vice versa)."
      )

  defp endianness_desc(:big),
    do: gettext("Each code unit is read high-order byte first (`Big Endian`).")

  defp endianness_desc(:little),
    do: gettext("Each code unit is read low-order byte first (`Little Endian`).")

  defp format_desc(1),
    do:
      gettext(
        "2 bytes = 1 code unit in the [BMP^Basic Multilingual Plane] (range `U+0000` → `U+FFFF`)."
      )

  defp format_desc(2),
    do:
      gettext(
        "4 bytes = surrogate pair for a code point beyond the [BMP^Basic Multilingual Plane]. The high code unit is in `0xD800` → `0xDBFF`, the low one in `0xDC00` → `0xDFFF`."
      )

  defp binary_desc(1), do: gettext("The 16 bits of the code unit are the code point.")

  defp binary_desc(2),
    do: gettext("Concatenate the 10 + 10 bits then add `0x10000` to recover the code point.")

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/decode/utf-16"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("UTF-16 sandbox (decode)")}
          subtitle={
            gettext(
              "Convert a sequence of UTF-16 bytes into a Unicode code point, given the endianness."
            )
          }
        />

        <.sandbox_input
          id="sb-bytes"
          label={gettext("Bytes")}
          value={@raw_input}
          placeholder={gettext("00 E9, D8 3C DF 89, or C3A9")}
          error={@error}
        >
          <:help>
            <.inline_desc text={
              gettext("Hex (with or without `0x`). UTF-16 takes 2 or 4 bytes per code point.")
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

            <.sandbox_step
              :if={@format_step}
              number={2}
              title={gettext("Identify the code unit count")}
            >
              <:desc><.inline_desc text={format_desc(div(length(@bytes), 2))} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="text-base text-accent font-medium">
                  {format_choice_label(@format_step.expected)}
                </span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@bit_groups_step}
              number={3}
              title={gettext("Extract the useful bits from each surrogate")}
            >
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Subtract `0xD800` from the high surrogate (10 high bits) and `0xDC00` from the low surrogate (10 low bits)."
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{Enum.join(@bit_groups_step.expected, " | ")}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@binary_step}
              number={(@bit_groups_step && 4) || 3}
              title={gettext("Reassemble the binary")}
            >
              <:desc><.inline_desc text={binary_desc(div(length(@bytes), 2))} /></:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step
              :if={@code_point_step}
              number={(@bit_groups_step && 5) || 4}
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
