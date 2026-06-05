defmodule AppWeb.SandboxLive.Latin1Encode do
  @moduledoc """
  Latin-1 encode sandbox: the simplest module - 1:1 mapping over
  U+0000..U+00FF, 2 steps.
  """
  use AppWeb, :live_view

  import AppWeb.SandboxComponents

  alias Charset.Encoding.Codec
  alias Charset.Encoding.CodePoint
  alias Charset.Exercise.Step
  alias Charset.Sandbox
  alias Charset.Sandbox.CodePointLabels
  alias Charset.Sandbox.InputParser

  @path "/sandbox/encode/latin1"
  @default_input "U+00E9"

  @impl true
  def mount(_params, _session, socket) do
    {:ok, assign(socket, page_title: gettext("Latin-1 sandbox (encode)"))}
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
         {:ok, _bytes} <- encode(code_point) do
      steps = Sandbox.encode_latin1(code_point)
      label = CodePointLabels.lookup(code_point)
      hex_bytes = Enum.find(steps, &match?(%Step.HexBytes{}, &1))

      assign(socket,
        error: nil,
        code_point: code_point,
        code_point_label: CodePoint.format(code_point),
        glyph: if(label == nil, do: <<code_point::utf8>>),
        label: label,
        binary_step: Enum.find(steps, &match?(%Step.Binary{}, &1)),
        bytes: hex_bytes.expected
      )
    else
      {:error, reason} ->
        assign(socket, error: error_message(reason), code_point: nil)
    end
  end

  defp encode(code_point) do
    case Codec.encode(code_point, :latin1) do
      {:ok, bytes} -> {:ok, bytes}
      {:error, _encode_error} -> {:error, :not_encodable}
    end
  end

  defp error_message(:empty), do: gettext("Enter a code point.")
  defp error_message(:unparseable), do: gettext("Unrecognised format.")
  defp error_message(:out_of_range), do: gettext("Outside the Unicode range (0 → U+10FFFF).")

  defp error_message(:surrogate),
    do: gettext("Surrogates (U+D800 → U+DFFF) are not valid Unicode scalar values.")

  defp error_message(:not_encodable),
    do: gettext("Code point outside the Latin-1 range (beyond `U+00FF`).")

  @impl true
  def render(assigns) do
    ~H"""
    <Layouts.sandbox
      flash={@flash}
      locale={@locale}
      alternate_path={@alternate_path}
      active="/sandbox/encode/latin1"
    >
      <main class="min-w-0">
        <.sandbox_header
          title={gettext("Latin-1 sandbox (encode)")}
          subtitle={
            gettext(
              "Convert a Unicode code point to a Latin-1 (ISO 8859-1) byte. 1:1 mapping over the `U+0000` → `U+00FF` range."
            )
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
          </div>
        </section>

        <%!-- Latin-1 is the simplest sandbox module - just 2 steps: read
             the bits, then convert to hex. --%>
        <section :if={@code_point}>
          <h2 class="font-mono text-xs uppercase tracking-widest text-mute mb-5">
            {gettext("Step-by-step breakdown")}
          </h2>

          <ol class="flex flex-col gap-0">
            <.sandbox_step :if={@binary_step} number={1} title={gettext("Convert to binary")}>
              <:desc>
                <.inline_desc text={
                  gettext(
                    "Code point `%{cp}` fits in exactly 8 bits. Latin-1 makes the code point value coincide with the byte value: no marker, no splitting.",
                    cp: @code_point_label
                  )
                } />
              </:desc>
              <div class="surface px-5 py-3 inline-block">
                <span class="hex text-base">{@binary_step.expected}</span>
              </div>
            </.sandbox_step>

            <.sandbox_step number={2} title={gettext("Convert to hexadecimal")} last>
              <:desc>
                <.inline_desc text={gettext("The 8 bits form a single byte, i.e. `2` hex digits.")} />
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
