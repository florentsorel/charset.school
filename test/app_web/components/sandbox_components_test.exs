defmodule AppWeb.SandboxComponentsTest do
  use ExUnit.Case, async: true

  import Phoenix.Component
  import Phoenix.LiveViewTest

  alias AppWeb.SandboxComponents

  defp render_inline_desc(text) do
    assigns = %{text: text}

    rendered = ~H"""
    <SandboxComponents.inline_desc text={@text} />
    """

    rendered |> Phoenix.HTML.Safe.to_iodata() |> IO.iodata_to_binary()
  end

  describe "inline_desc/1" do
    test "backtick spans become inline <code>" do
      assert render_inline_desc("with or without `0x`") =~
               ~s(with or without <code class="inline-code">0x</code>)
    end

    test "[text^title] becomes <abbr>" do
      assert render_inline_desc("the [BMP^Basic Multilingual Plane] covers") =~
               ~s(<abbr title="Basic Multilingual Plane">BMP</abbr>)
    end

    test "newlines become <br>" do
      assert render_inline_desc("line one\nline two") =~ "line one<br/>line two"
    end

    test "mixes all three in one string" do
      html = render_inline_desc("Range `U+0000`\nsee [CJK^Chinese, Japanese, Korean]")

      assert html =~ ~s(<code class="inline-code">U+0000</code>)
      assert html =~ "<br/>"
      assert html =~ ~s(<abbr title="Chinese, Japanese, Korean">CJK</abbr>)
    end

    test "escapes HTML inside markup tokens and plain text" do
      html = render_inline_desc("a <b> tag and `<code>` and [x<y^a > b]")

      refute html =~ "<b>"
      assert html =~ "a &lt;b&gt; tag"
      assert html =~ ~s(<code class="inline-code">&lt;code&gt;</code>)
      assert html =~ ~s(<abbr title="a &gt; b">x&lt;y</abbr>)
    end
  end
end
