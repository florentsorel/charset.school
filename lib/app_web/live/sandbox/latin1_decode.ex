defmodule AppWeb.SandboxLive.Latin1Decode do
  @moduledoc """
  Latin-1 decode sandbox: one byte in, its code point out - perfect identity.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.BytesParser
  alias Charset.Sandbox.CodePointLabels

  @path "/sandbox/decode/latin1"
  @default_input "E9"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("Latin-1 sandbox (decode)"))}
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
      steps = Sandbox.decode_latin1(bytes)
      label = CodePointLabels.lookup(code_point)

      assign(socket,
        error: nil,
        bytes: :binary.bin_to_list(bytes),
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
    case Codec.decode(bytes, :latin1) do
      {:ok, code_point} -> {:ok, code_point}
      {:error, _decode_error} -> {:error, :decoder}
    end
  end

  defp error_message(:empty), do: gettext("Enter a hex byte.")
  defp error_message(:invalid_hex), do: gettext("Non-hex characters detected.")

  defp error_message(:odd_length),
    do: gettext("The byte is incomplete (odd number of hex digits).")

  defp error_message(:too_long),
    do: gettext("Too many bytes: Latin-1 encodes a code point in exactly 1 byte.")

  defp error_message(:decoder), do: gettext("This byte is not a valid Latin-1 byte.")

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/decode/latin1"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("Latin-1 sandbox (decode)")}
          subtitle={
            gettext(
              "Convert a Latin-1 (ISO 8859-1) byte into a Unicode code point. 1:1 mapping, perfect identity over `0x00` → `0xFF`."
            )
          }
        />

        <.sandbox_input
          id="sb-bytes"
          label={gettext("Byte")}
          value={@raw_input}
          placeholder={gettext("E9, 0xE9, or 41")}
          error={@error}
        >
          <:help>
            <.inline_desc text={
              gettext(
                "A single hex byte (with or without `0x`). Latin-1 is strictly 1 byte = 1 code point."
              )
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
          </div>
        </section>

        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step :if={@binary_step} number={1} title={gettext("Read the byte's bits")}>
              <:desc>
                <.inline_desc text={
                  gettext("The byte is directly the 8 bits of the code point (no marker to strip).")
                } />
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
                  gettext(
                    "The byte equals `%{decimal}` in decimal, i.e. `%{cp}` in Unicode notation. Latin-1 makes the byte value and the code point coincide over the whole range.",
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
