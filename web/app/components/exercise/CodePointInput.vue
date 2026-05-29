<script setup lang="ts">
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
  return n.toString(16).toUpperCase().padStart(4, '0')
}

function onInput(ev: Event) {
  const target = ev.target as HTMLInputElement
  const cleaned = target.value.replace(/^U\+/i, '').replace(/[^0-9a-fA-F]/g, '').toUpperCase()
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
  <div class="code-point-input">
    <span class="prefix">U+</span>
    <input
      class="code-point-cell"
      type="text"
      inputmode="text"
      autocapitalize="characters"
      :value="raw"
      :disabled="disabled"
      :aria-label="$t('exercise.code_point_input_label')"
      @input="onInput"
    >
  </div>
</template>

<style scoped>
.code-point-input {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.4rem 0.6rem;
  background: var(--color-surface);
  border: 1px solid var(--color-rule);
  border-radius: 6px;
}
.code-point-input:focus-within {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}
.prefix {
  font-family: var(--font-mono);
  color: var(--color-mute);
  font-size: 0.95rem;
}
.code-point-cell {
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
.code-point-cell:disabled {
  opacity: 0.5;
}
</style>
