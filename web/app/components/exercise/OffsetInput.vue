<script setup lang="ts">
// Hex entry for the UTF-16 "subtract 0x10000" step: the user types the 20-bit
// result (code point - 0x10000). Mirrors CodePointInput but with a `0x` prefix
// and no fixed-width zero padding.
const props = defineProps<{
  modelValue: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const raw = ref(props.modelValue != null ? formatHex(props.modelValue) : '')

watch(() => props.modelValue, (v) => {
  raw.value = v != null ? formatHex(v) : ''
})

function formatHex(n: number): string {
  return n.toString(16).toUpperCase()
}

function onInput(ev: Event) {
  const target = ev.target as HTMLInputElement
  const cleaned = target.value.replace(/^0x/i, '').replace(/[^0-9a-fA-F]/g, '').toUpperCase()
  raw.value = cleaned
  if (cleaned.length === 0) {
    emit('update:modelValue', null)
    return
  }
  const value = parseInt(cleaned, 16)
  emit('update:modelValue', isNaN(value) ? null : value)
}
</script>

<template>
  <div class="offset-input">
    <span class="prefix">0x</span>
    <input
      class="offset-cell"
      type="text"
      inputmode="text"
      autocapitalize="characters"
      :value="raw"
      :disabled="disabled"
      :aria-label="$t('exercise.offset_input_label')"
      @input="onInput"
    >
  </div>
</template>

<style scoped>
.offset-input {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.4rem 0.6rem;
  background: var(--color-surface);
  border: 1px solid var(--color-rule);
  border-radius: 6px;
}
.offset-input:focus-within {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}
.prefix {
  font-family: var(--font-mono);
  color: var(--color-mute);
  font-size: 0.95rem;
}
.offset-cell {
  background: transparent;
  border: 0;
  outline: none;
  font-family: var(--font-mono);
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  width: 6ch;
  color: var(--color-ink);
}
.offset-cell:disabled {
  opacity: 0.5;
}
</style>
