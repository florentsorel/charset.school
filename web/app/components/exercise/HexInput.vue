<script setup lang="ts">
const UNFILLED = -1

const props = defineProps<{
  modelValue: number[]
  byteCount: number
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number[]]
}>()

const cells = computed(() => {
  return Array.from({ length: props.byteCount }, (_, i) => {
    const v = props.modelValue[i]
    return v != null && v >= 0 ? v.toString(16).toUpperCase().padStart(2, '0') : ''
  })
})

const refs = ref<HTMLInputElement[]>([])

function setCell(index: number, hex: string) {
  const next = Array.from({ length: props.byteCount }, (_, i) => props.modelValue[i] ?? UNFILLED)
  next[index] = hex.length === 2 ? parseInt(hex, 16) : UNFILLED
  emit('update:modelValue', next)
}

function onInput(index: number, ev: Event) {
  const target = ev.target as HTMLInputElement
  const cleaned = target.value.replace(/[^0-9a-fA-F]/g, '').toUpperCase().slice(0, 2)
  target.value = cleaned
  if (cleaned.length === 2) {
    setCell(index, cleaned)
    focusNext(index)
  }
}

function onKeydown(index: number, ev: KeyboardEvent) {
  const target = ev.target as HTMLInputElement
  if (ev.key === 'Backspace') {
    if (!target.value) {
      ev.preventDefault()
      focusPrev(index)
    }
  } else if (ev.key === 'ArrowLeft' && target.selectionStart === 0) {
    ev.preventDefault()
    focusPrev(index)
  } else if (ev.key === 'ArrowRight' && target.selectionStart === target.value.length) {
    ev.preventDefault()
    focusNext(index)
  }
}

function focusNext(index: number) {
  refs.value[Math.min(props.byteCount - 1, index + 1)]?.focus()
}

function focusPrev(index: number) {
  const prev = refs.value[Math.max(0, index - 1)]
  if (prev) {
    prev.focus()
    prev.setSelectionRange(prev.value.length, prev.value.length)
  }
}
</script>

<template>
  <span class="hex-row">
    <input
      v-for="(cell, i) in cells"
      :key="i"
      :ref="el => { if (el) refs[i] = el as HTMLInputElement }"
      class="hex-cell"
      type="text"
      inputmode="text"
      maxlength="2"
      autocapitalize="characters"
      :value="cell"
      :disabled="disabled"
      :aria-label="`Octet ${i + 1}`"
      @input="onInput(i, $event)"
      @keydown="onKeydown(i, $event)"
    >
  </span>
</template>

<style scoped>
.hex-row {
  display: inline-flex;
  gap: 6px;
}
.hex-cell {
  width: 2.5rem;
  height: 2rem;
  border: 1px solid var(--color-rule);
  border-radius: 4px;
  background: var(--color-surface);
  color: var(--color-ink);
  font-family: var(--font-mono);
  font-size: 0.95rem;
  font-weight: 600;
  text-align: center;
  outline: none;
  letter-spacing: 0.05em;
}
.hex-cell:focus {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}
.hex-cell:disabled {
  opacity: 0.5;
}
</style>
