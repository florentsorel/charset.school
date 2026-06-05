defmodule CharsetWeb.PageHTML do
  @moduledoc """
  This module contains pages rendered by PageController.

  See the `page_html` directory for all templates available.
  """
  use CharsetWeb, :html

  embed_templates "page_html/*"

  # The landing aperçu bullets, translated at render time.
  defp bullets do
    [
      gettext("Step-by-step validation, not just at the end"),
      gettext("Errors explained in plain English, not error codes"),
      gettext("Code points drawn at random - no end"),
      gettext("UTF-8, UTF-16, UTF-32 - encode and decode, with graded levels")
    ]
  end

  # Hero demo: U+1F389 (🎉) encoded to UTF-8, byte by byte. Markers are
  # highlighted (4-byte leader 11110, continuation 10) - same data as the old
  # HeroDemo.vue.
  defp hero_demo(assigns) do
    assigns =
      assign(assigns, :bytes, [
        %{bits: "11110000", marker_len: 5, cont: false},
        %{bits: "10011111", marker_len: 2, cont: true},
        %{bits: "10001110", marker_len: 2, cont: true},
        %{bits: "10001001", marker_len: 2, cont: true}
      ])

    ~H"""
    <div class="surface p-6 w-full" aria-hidden="true">
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-4">
        {gettext("example · encode UTF-8")}
      </p>

      <div class="flex items-baseline gap-3 mb-5">
        <span class="codepoint-glyph leading-none" style="font-size: 22px;">U+1F389</span>
        <span class="leading-none text-mute" style="font-size: 22px;">🎉</span>
      </div>

      <div class="flex flex-col gap-1.5 mb-5">
        <div :for={byte <- @bytes} class="bit-row" style="gap: 2px;">
          <span
            :for={{bit, idx} <- Enum.with_index(String.graphemes(byte.bits))}
            class={["bit", bit_class(byte, idx)]}
            style="width: 1.5rem; height: 1.85rem; font-size: 13px;"
          >
            {bit}
          </span>
        </div>
      </div>

      <p class="hex text-base font-medium">
        0xF0 0x9F 0x8E 0x89
      </p>
    </div>
    """
  end

  defp bit_class(%{cont: true}, 0), do: "bit-cont"
  defp bit_class(%{cont: true}, 1), do: "bit-boundary"
  defp bit_class(%{cont: true}, _idx), do: "bit-payload"
  defp bit_class(%{marker_len: 0}, _idx), do: "bit-payload"
  defp bit_class(%{marker_len: len}, idx) when idx < len - 1, do: "bit-marker"
  defp bit_class(%{marker_len: len}, idx) when idx == len - 1, do: "bit-boundary"
  defp bit_class(_byte, _idx), do: "bit-payload"

  # Static "coach correcting a mistake" preview for the landing aperçu section.
  # Mirrors the real BitGroups step ("split the useful bits") for U+00E9 (é,
  # 2 bytes): the UTF-8 markers (110 / 10) are shown as FIXED context - the
  # user only fills the useful payload bits - and the coach catches a wrong
  # payload bit, not a marker. Illustrates "a coach that corrects you bit by
  # bit" without re-doing the hero's encoding demo.
  defp coach_preview(assigns) do
    assigns =
      assign(assigns,
        # Byte 0 (target 0xC3 = 11000011): marker `110` then payload `00011`.
        # The user got the last payload bit wrong (typed 0, should be 1).
        byte0: [
          {"1", "marker"},
          {"1", "marker"},
          {"0", "boundary"},
          {"0", "payload"},
          {"0", "payload"},
          {"0", "payload"},
          {"1", "payload"},
          {"0", "wrong"}
        ],
        # Byte 1 (0xA9 = 10101001): cont marker `10` then payload `101001` - correct.
        byte1: [
          {"1", "cont"},
          {"0", "boundary"},
          {"1", "payload"},
          {"0", "payload"},
          {"1", "payload"},
          {"0", "payload"},
          {"0", "payload"},
          {"1", "payload"}
        ]
      )

    ~H"""
    <div class="surface p-6 w-full" aria-hidden="true">
      <p class="font-mono text-xs uppercase tracking-widest text-faint mb-4">
        {gettext("encode UTF-8 · split the useful bits")}
      </p>

      <div class="flex items-baseline gap-3 mb-4">
        <span class="codepoint-glyph leading-none" style="font-size: 20px;">U+00E9</span>
        <span class="leading-none text-mute" style="font-size: 20px;">é</span>
      </div>

      <div class="flex flex-col gap-1.5 mb-5">
        <div :for={byte <- [@byte0, @byte1]} class="bit-row" style="gap: 2px;">
          <span
            :for={{bit, kind} <- byte}
            class={["bit", "bit-#{kind}"]}
            style="width: 1.5rem; height: 1.85rem; font-size: 13px;"
          >
            {bit}
          </span>
        </div>
      </div>

      <div class="coach-tip">
        <.icon name="hero-information-circle" class="coach-tip-icon" />
        <p class="coach-tip-text">
          {gettext(
            "The markers (110, 10) are fixed - you only fill the payload. Right size, wrong bits: recheck how you split the code point."
          )}
        </p>
      </div>
    </div>
    """
  end
end
