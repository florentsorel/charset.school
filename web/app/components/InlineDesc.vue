<script setup lang="ts">
/**
 * Renders a short i18n description with three light Markdown-ish features:
 *   - backtick spans become inline `<code>` (e.g. `0xD800`)
 *   - `[text^title]` becomes `<abbr title="title">text</abbr>` (e.g.
 *     `[BMP^Basic Multilingual Plane]`). We use `^` as the separator
 *     because both `|` (plural delimiter) and `@` (linked message
 *     prefix) have reserved meanings in vue-i18n message syntax and
 *     would trigger parse errors / silent truncation.
 *   - newline characters (`\n`) become `<br>` so multi-line strings
 *     remain readable without breaking out of the `<p>` they live in
 *
 * Keeps the i18n catalogs free of HTML (writers use a tiny ASCII syntax).
 */
type Token
  = | { kind: 'text', value: string }
    | { kind: 'code', value: string }
    | { kind: 'abbr', value: string, title: string }
    | { kind: 'br' }

const props = defineProps<{
  text: string
}>()

// Single regex captures both abbr `[text^title]` and code `` `text` ``
// patterns. Group 1+2 = abbr; group 3 = code. Iterated via `matchAll`
// (stateless) to avoid `lastIndex` leaking across concurrent calls when
// multiple InlineDesc instances share this module-level constant.
const INLINE_PATTERN = /\[([^\]^]+)\^([^\]]+)\]|`([^`]+)`/g

function tokenize(input: string): Token[] {
  const tokens: Token[] = []
  input.split('\n').forEach((line, lineIdx) => {
    if (lineIdx > 0) tokens.push({ kind: 'br' })
    let pos = 0
    for (const match of line.matchAll(INLINE_PATTERN)) {
      const matchStart = match.index ?? 0
      if (matchStart > pos) {
        tokens.push({ kind: 'text', value: line.slice(pos, matchStart) })
      }
      if (match[1] !== undefined && match[2] !== undefined) {
        tokens.push({ kind: 'abbr', value: match[1], title: match[2] })
      } else if (match[3] !== undefined) {
        tokens.push({ kind: 'code', value: match[3] })
      }
      pos = matchStart + match[0].length
    }
    if (pos < line.length) {
      tokens.push({ kind: 'text', value: line.slice(pos) })
    }
  })
  return tokens
}

const tokens = computed(() => tokenize(props.text))
</script>

<template>
  <span>
    <template
      v-for="(token, i) in tokens"
      :key="i"
    >
      <br v-if="token.kind === 'br'">
      <code
        v-else-if="token.kind === 'code'"
        class="inline-code"
      >{{ token.value }}</code>
      <abbr
        v-else-if="token.kind === 'abbr'"
        :title="token.title"
      >{{ token.value }}</abbr>
      <template v-else>{{ token.value }}</template>
    </template>
  </span>
</template>
